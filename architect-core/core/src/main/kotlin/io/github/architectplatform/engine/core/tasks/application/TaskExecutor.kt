package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.execution.TaskPermissionScope
import io.github.architectplatform.engine.core.events.EventBus
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.domain.TaskDependencyResolver
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskCompletedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskFailedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskSkippedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskStartedEvent
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.github.architectplatform.engine.domain.events.generateExecutionId
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Executes tasks with dependency resolution, caching, and parallel batch execution.
 *
 * Tasks are grouped into parallel batches by the [TaskDependencyResolver]. Tasks within
 * the same batch have no ordering dependency on each other and run concurrently when
 * [parallelExecutionEnabled] is true. Batches themselves execute sequentially.
 */
@Singleton
class TaskExecutor(
    private val environment: Environment,
    private val taskCache: TaskCache,
    private val eventBus: EventBus<ArchitectEvent<*>>,
    private val dependencyResolver: TaskDependencyResolver = TaskDependencyResolver(),
    private val parallelExecutionEnabled: Boolean = EngineConfiguration.TaskExecution.DEFAULT_PARALLEL_ENABLED,
    private val outputCache: LocalOutputCache? = null,
    private val outputCacheEnabled: Boolean = false,
    private val remoteOutputCache: RemoteOutputCache? = null,
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

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

      val allResults = mutableListOf<TaskResult>()
      for ((batchIndex, batchTasks) in tasksByBatch) {
        if (parallelExecutionEnabled && batchTasks.size > 1) {
          logger.debug("Executing batch $batchIndex with ${batchTasks.size} tasks in parallel: ${batchTasks.map { it.id }}")
        }
        val batchResults = executeBatch(batchTasks, executionId, projectName, projectContext, args, taskRegistry, parentProject)
        allResults.addAll(batchResults)
        if (batchResults.any { !it.success }) break
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

  private fun executeSingleTask(
      currentTask: Task,
      executionId: ExecutionId,
      projectName: String,
      projectContext: ProjectContext,
      args: List<String>,
      taskRegistry: TaskRegistry,
      parentProject: String?,
  ): TaskResult {
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

    eventBus(taskStartedEvent(projectName, executionId, currentTask.id, message = "Starting task: ${currentTask.id}", subProject = parentProject))
    return try {
      val result = TaskPermissionScope.withTask(currentTask) {
        currentTask.execute(environment, projectContext, args)
      }
      val childResults = if (currentTask.children().isNotEmpty()) {
        dependencyResolver.resolveChildren(currentTask, taskRegistry).map { child ->
          eventBus(taskStartedEvent(projectName, executionId, child.id, message = "Starting child task: ${child.id} (parent: ${currentTask.id})", subProject = parentProject))
          val childResult = TaskPermissionScope.withTask(child) {
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

      val finalResult = if (childResults.isNotEmpty()) {
        val combined = listOf(result) + childResults
        val failedCount = combined.count { !it.success }
        if (combined.all { it.success }) {
          TaskResult.success(result.message ?: "Task ${currentTask.id} and ${childResults.size} children completed", combined)
        } else {
          TaskResult.failure("$failedCount task(s) failed for ${currentTask.id}", combined)
        }
      } else result

      logger.debug("Executed task '${currentTask.id}' with result: $finalResult")
      if (finalResult.success) {
        eventBus(taskCompletedEvent(projectName, executionId, currentTask.id, message = finalResult.message ?: "Task ${currentTask.id} completed successfully", subProject = parentProject))
      } else {
        val errMsg = finalResult.message ?: "Task failed without message"
        logger.error("Task '${currentTask.id}' failed in project '$projectName': $errMsg")
        eventBus(taskFailedEvent(projectName, executionId, currentTask.id, message = errMsg, errorDetails = errMsg, parentProject = parentProject))
      }
      taskCache.store(currentTask.id, finalResult)

      // Store in content-hash output cache on success
      if (outputCacheEnabled && outputCache != null && finalResult.success) {
        val descriptor = currentTask.cacheDescriptor()
        if (descriptor != null) {
          val cacheKey = CacheKeyComputer.compute(descriptor, projectContext.dir.toString(), projectContext.config)
          outputCache.store(cacheKey, finalResult, stdout = finalResult.message)
          remoteOutputCache?.storeResult(cacheKey, finalResult, stdout = finalResult.message)
        }
      }

      finalResult
    } catch (e: Exception) {
      val errMsg = e.message ?: "Unknown error"
      val stack = e.stackTraceToString()
      eventBus(taskFailedEvent(projectName, executionId, currentTask.id, message = "Task '${currentTask.id}' failed with exception: $errMsg", errorDetails = "Exception: $errMsg\n\nStack Trace:\n$stack", parentProject = parentProject))
      logger.error("Exception during execution of task '${currentTask.id}'", e)
      TaskResult.failure("Task '${currentTask.id}' failed with exception: $errMsg")
    }
  }
}
