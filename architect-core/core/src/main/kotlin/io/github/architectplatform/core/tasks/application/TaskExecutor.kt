package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.FailureStrategy
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.core.execution.TaskPermissionScope
import io.github.architectplatform.core.events.EventBus
import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.tasks.domain.TaskDependencyResolver
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskCompletedEvent
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskFailedEvent
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskRetryingEvent
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskSkippedEvent
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskStartedEvent
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionId
import io.github.architectplatform.core.domain.events.generateExecutionId
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory

/**
 * Executes tasks with dependency resolution, caching, and parallel batch execution.
 *
 * Tasks are grouped into parallel batches by the [TaskDependencyResolver]. Tasks within
 * the same batch have no ordering dependency on each other and run concurrently when
 * [parallelExecutionEnabled] is true. Batches themselves execute sequentially.
 */
class TaskExecutor(
    private val environment: Environment,
    private val taskCache: TaskCache,
    private val eventBus: EventBus<ArchitectEvent<*>>,
    private val dependencyResolver: TaskDependencyResolver = TaskDependencyResolver(),
    private val parallelExecutionEnabled: Boolean = EngineConfiguration.TaskExecution.DEFAULT_PARALLEL_ENABLED,
    private val maxConcurrentTasks: Int = EngineConfiguration.TaskExecution.DEFAULT_MAX_CONCURRENT_TASKS,
    private val outputCache: LocalOutputCache? = null,
    private val outputCacheEnabled: Boolean = false,
    private val remoteOutputCache: RemoteOutputCache? = null,
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  // Semaphore to bound the number of tasks executing concurrently across all parallel batches.
  // A value <= 0 means unlimited (no semaphore created).
  private val concurrencySemaphore: Semaphore? =
    if (maxConcurrentTasks > 0) Semaphore(maxConcurrentTasks) else null

  fun execute(
      project: Project,
      task: Task,
      context: ProjectContext,
      args: List<String>,
      parentProject: String? = null,
      executionId: ExecutionId? = null,
  ): Pair<ExecutionId, Deferred<TaskResult>> {
    val actualExecutionId = executionId ?: generateExecutionId()
    val deferred = CoroutineScope(Dispatchers.IO).async {
      syncExecuteTask(actualExecutionId, task, context, args, project.taskRegistry, parentProject)
    }
    return actualExecutionId to deferred
  }

  private suspend fun syncExecuteTask(
      executionId: ExecutionId,
      task: Task,
      projectContext: ProjectContext,
      args: List<String>,
      taskRegistry: TaskRegistry,
      parentProject: String? = null,
  ): TaskResult {
    val projectName = projectContext.config.getKey<String>("project.name") ?: "unknown"
    return try {
      val allTasks = dependencyResolver.resolveAllDependencies(task, taskRegistry)
      val executionOrder = dependencyResolver.topologicalSort(allTasks)
      val batches = dependencyResolver.toBatches(executionOrder)
      val tasksByBatch = executionOrder.groupBy { batches[it.id] ?: 0 }.toSortedMap()

      // Collect upstream task data as tasks complete for inter-task data passing.
      val upstreamData = mutableMapOf<String, Map<String, Any>>()

      // Batches run sequentially (deps satisfied); tasks within a batch run in parallel.
      val allResults = mutableListOf<TaskResult>()
      for ((batchIndex, batchTasks) in tasksByBatch) {
        if (parallelExecutionEnabled && batchTasks.size > 1) {
          logger.debug("Executing batch $batchIndex with ${batchTasks.size} tasks in parallel: ${batchTasks.map { it.id }}")
        }
        val batchResults = executeBatch(batchTasks, executionId, projectName, projectContext, args, taskRegistry, parentProject)
        allResults.addAll(batchResults)
        // Collect data from completed tasks for downstream consumption.
        batchTasks.zip(batchResults).forEach { (t, r) ->
          if (r.data.isNotEmpty()) {
            upstreamData[t.id] = r.data
          }
        }
        // Abort only if any failed task uses ABORT strategy (default).
        // Tasks with CONTINUE strategy allow execution to proceed.
        val shouldAbort = batchTasks.zip(batchResults).any { (t, r) ->
          !r.success && t.onFailure() is FailureStrategy.ABORT
        }
        if (shouldAbort) break
      }

      if (allResults.all { it.success }) {
        TaskResult.success("All tasks completed successfully")
      } else {
        val failed = allResults.filter { !it.success }
        val msgs = failed.mapNotNull { it.message }.joinToString(", ")
        TaskResult.failure("Execution failed. ${failed.size} task(s) failed" + if (msgs.isNotEmpty()) ": $msgs" else "")
      }
    } catch (e: Exception) {
      val msg = e.message ?: "Unknown error"
      TaskResult.failure("Execution failed with exception: $msg\n\nStack Trace:\n${e.stackTraceToString()}")
    }
  }

  private suspend fun executeBatch(
      tasks: List<Task>,
      executionId: ExecutionId,
      projectName: String,
      projectContext: ProjectContext,
      args: List<String>,
      taskRegistry: TaskRegistry,
      parentProject: String?,
  ): List<TaskResult> {
    return if (parallelExecutionEnabled && tasks.size > 1) {
      coroutineScope {
        tasks.map { t -> async(Dispatchers.IO) { executeSingleTask(t, executionId, projectName, projectContext, args, taskRegistry, parentProject) } }.awaitAll()
      }
    } else {
      tasks.map { t -> executeSingleTask(t, executionId, projectName, projectContext, args, taskRegistry, parentProject) }
    }
  }

  private suspend fun executeSingleTask(
      currentTask: Task,
      executionId: ExecutionId,
      projectName: String,
      projectContext: ProjectContext,
      args: List<String>,
      taskRegistry: TaskRegistry,
      parentProject: String?,
  ): TaskResult {
    // Cache lookup order: task cache → local output cache → remote output cache.
    // On a remote hit, populate local cache so subsequent runs avoid network round-trips.
    if (taskCache.isCached(currentTask.id)) {
      eventBus(taskSkippedEvent(projectName, executionId, currentTask.id, message = "Task ${currentTask.id} skipped (cached)", subProject = parentProject))
      val cached = taskCache.get(currentTask.id)
      if (cached != null) {
        eventBus(taskCompletedEvent(projectName, executionId, currentTask.id, message = "Task ${currentTask.id} completed (from cache)", subProject = parentProject))
        return cached
      }
    }

    // Content-hash output cache: check LocalOutputCache, then remote, before executing
    if (outputCacheEnabled && outputCache != null) {
      val descriptor = currentTask.cacheDescriptor()
      if (descriptor != null) {
        val cacheKey = CacheKeyComputer.compute(descriptor, projectContext.dir.toString(), projectContext.config)
        val cachedResult = outputCache.get(cacheKey)
          ?: remoteOutputCache?.fetchResult(cacheKey)?.let { remote ->
            // Populate local cache from remote hit
            val tr = remote.toTaskResult()
            outputCache.store(cacheKey, tr, stdout = remote.stdout)
            LocalOutputCache.CachedResult(success = remote.success, message = remote.message, stdout = remote.stdout)
          }
        if (cachedResult != null) {
          val taskResult = cachedResult.toTaskResult()
          taskCache.store(currentTask.id, taskResult)
          eventBus(taskSkippedEvent(projectName, executionId, currentTask.id, message = "Task ${currentTask.id} skipped (output cache hit)", subProject = parentProject))
          eventBus(taskCompletedEvent(projectName, executionId, currentTask.id, message = "Task ${currentTask.id} completed (output cache hit)", subProject = parentProject))
          return taskResult
        }
      }
    }

    // Runtime condition check: skip task if shouldExecute() returns false
    if (!currentTask.shouldExecute(environment, projectContext)) {
      val skipResult = TaskResult.skipped("Task '${currentTask.id}' skipped: shouldExecute() returned false")
      taskCache.store(currentTask.id, skipResult)
      eventBus(taskSkippedEvent(projectName, executionId, currentTask.id, message = "Task ${currentTask.id} skipped (condition not met)", subProject = parentProject))
      eventBus(taskCompletedEvent(projectName, executionId, currentTask.id, message = "Task ${currentTask.id} skipped (condition not met)", subProject = parentProject))
      return skipResult
    }

    eventBus(taskStartedEvent(projectName, executionId, currentTask.id, message = "Starting task: ${currentTask.id}", subProject = parentProject))

    val retryStrategy = currentTask.onFailure() as? FailureStrategy.RETRY
    val maxAttempts = if (retryStrategy != null) retryStrategy.maxAttempts + 1 else 1 // initial + retries

    // Acquire concurrency permit if a semaphore is configured.
    // This suspends when max concurrent tasks are already running.
    suspend fun executeWithSemaphore(block: suspend () -> TaskResult): TaskResult =
      if (concurrencySemaphore != null) concurrencySemaphore.withPermit { block() } else block()

    var lastResult: TaskResult = TaskResult.failure("Task '${currentTask.id}' did not execute")
    for (attempt in 1..maxAttempts) {
      // Apply backoff delay before retries (not before the first attempt)
      if (attempt > 1 && retryStrategy != null) {
        val delayMs = retryStrategy.computeDelayMs(attempt - 1) // attempt-1 = retry index (1-based)
        eventBus(taskRetryingEvent(projectName, executionId, currentTask.id, attempt, maxAttempts, delayMs, parentProject))
        logger.info("Retrying task '${currentTask.id}' (attempt $attempt/$maxAttempts)${if (delayMs > 0) " after ${delayMs}ms backoff" else ""}")
        if (delayMs > 0) delay(delayMs)
      }
      lastResult = executeWithSemaphore {
        try {
        val taskTimeout = currentTask.timeout()
        val result = if (taskTimeout != null) {
          val executor = Executors.newSingleThreadExecutor()
          try {
            val future = executor.submit(Callable {
              TaskPermissionScope.withTask(currentTask, projectContext.dir) {
                currentTask.execute(environment, projectContext, args)
              }
            })
            future.get(taskTimeout.toMillis(), TimeUnit.MILLISECONDS)
          } catch (e: TimeoutException) {
            TaskResult.failure("Task '${currentTask.id}' timed out after ${taskTimeout.seconds}s")
          } finally {
            executor.shutdownNow()
          }
        } else {
          TaskPermissionScope.withTask(currentTask, projectContext.dir) {
            currentTask.execute(environment, projectContext, args)
          }
        }
        // Children execute after parent succeeds; results merge into a composite TaskResult.
        val childResults = if (currentTask.children().isNotEmpty()) {
          dependencyResolver.resolveChildren(currentTask, taskRegistry).map { child ->
            eventBus(taskStartedEvent(projectName, executionId, child.id, message = "Starting child task: ${child.id} (parent: ${currentTask.id})", subProject = parentProject))
            val childResult = TaskPermissionScope.withTask(child, projectContext.dir) {
              child.execute(environment, projectContext, args)
            }
            if (childResult.success) {
              eventBus(taskCompletedEvent(projectName, executionId, child.id, message = childResult.message ?: "Child task ${child.id} completed", subProject = parentProject))
            } else {
              eventBus(taskFailedEvent(projectName, executionId, child.id, message = childResult.message ?: "Child task ${child.id} failed", errorDetails = childResult.message ?: "", parentProject = parentProject))
            }
            taskCache.store(child.id, childResult)
            childResult
          }
        } else emptyList()

        if (childResults.isNotEmpty()) {
          val combined = listOf(result) + childResults
          val failedCount = combined.count { !it.success }
          if (combined.all { it.success }) {
            TaskResult.success(result.message ?: "Task ${currentTask.id} and ${childResults.size} children completed", combined)
          } else {
            TaskResult.failure("$failedCount task(s) failed for ${currentTask.id}", combined)
          }
        } else result
      } catch (e: Exception) {
        val errMsg = e.message ?: "Unknown error"
        logger.error("Exception during execution of task '${currentTask.id}' (attempt $attempt/$maxAttempts)", e)
        TaskResult.failure("Task '${currentTask.id}' failed with exception: $errMsg")
      }
      } // end executeWithSemaphore

      if (lastResult.success || attempt == maxAttempts) break
    }

    logger.debug("Executed task '${currentTask.id}' with result: $lastResult")
    if (lastResult.success) {
      eventBus(taskCompletedEvent(projectName, executionId, currentTask.id, message = lastResult.message ?: "Task ${currentTask.id} completed successfully", subProject = parentProject))
    } else {
      val errMsg = lastResult.message ?: "Task failed without message"
      logger.error("Task '${currentTask.id}' failed in project '$projectName': $errMsg")
      eventBus(taskFailedEvent(projectName, executionId, currentTask.id, message = errMsg, errorDetails = errMsg, parentProject = parentProject))
    }
    taskCache.store(currentTask.id, lastResult)

    // Store in content-hash output cache on success
    if (outputCacheEnabled && outputCache != null && lastResult.success) {
      val descriptor = currentTask.cacheDescriptor()
      if (descriptor != null) {
        val cacheKey = CacheKeyComputer.compute(descriptor, projectContext.dir.toString(), projectContext.config)
        outputCache.store(cacheKey, lastResult, stdout = lastResult.message)
        remoteOutputCache?.storeResult(cacheKey, lastResult, stdout = lastResult.message)
      }
    }

    return lastResult
  }
}
