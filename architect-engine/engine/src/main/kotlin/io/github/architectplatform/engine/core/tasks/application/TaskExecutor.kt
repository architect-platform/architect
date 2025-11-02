package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.domain.TaskDependencyResolver
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents.executionCompletedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents.executionFailedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents.executionStartedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskCompletedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskFailedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskSkippedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents.taskStartedEvent
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.github.architectplatform.engine.domain.events.generateExecutionId
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory

/**
 * Executes tasks with dependency resolution and caching support.
 * 
 * This executor is responsible for:
 * - Executing individual tasks within their project context
 * - Resolving and respecting task dependencies
 * - Managing task result caching
 * - Publishing execution events
 */
@Singleton
@ExecuteOn(TaskExecutors.BLOCKING)
class TaskExecutor(
    private val environment: Environment,
    private val taskCache: TaskCache,
    private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>,
    private val dependencyResolver: TaskDependencyResolver = TaskDependencyResolver()
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun execute(
      project: Project,
      task: Task,
      context: ProjectContext,
      args: List<String>,
      parentProject: String? = null,
      executionId: ExecutionId? = null
  ): Pair<ExecutionId, Deferred<TaskResult>> {
    val actualExecutionId = executionId ?: generateExecutionId()
      val deferred = CoroutineScope(Dispatchers.IO).async {
              syncExecuteTask(
                  actualExecutionId,
                  task,
                  context,
                  args,
                  project.taskRegistry,
                  parentProject,
              )
      }

      return actualExecutionId to deferred
  }

  private fun syncExecuteTask(
      executionId: ExecutionId,
      task: Task,
      projectContext: ProjectContext,
      args: List<String>,
      taskRegistry: TaskRegistry,
      parentProject: String? = null,
  ): TaskResult {
    val projectName = projectContext.config.getKey<String>("project.name") ?: "unknown"

    try {

      val allTasks = dependencyResolver.resolveAllDependencies(task, taskRegistry)
      val executionOrder = dependencyResolver.topologicalSort(allTasks)
      val results =
          executionOrder
              .map { currentTask ->
                if (taskCache.isCached(currentTask.id)) {
                  eventPublisher.publishEvent(
                      taskSkippedEvent(
                          projectName,
                          executionId,
                          currentTask.id,
                          message = "Task ${currentTask.id} skipped (cached)",
                          subProject = parentProject,
                      ))
                  val cachedResult = taskCache.get(currentTask.id)
                  if (cachedResult != null) {
                    eventPublisher.publishEvent(
                        taskCompletedEvent(
                            projectName,
                            executionId,
                            currentTask.id,
                            message = "Task ${currentTask.id} completed (from cache)",
                            subProject = parentProject))
                    return@map cachedResult
                  }
                }

                eventPublisher.publishEvent(
                    taskStartedEvent(
                        projectName,
                        executionId,
                        currentTask.id,
                        message = "Starting task: ${currentTask.id}",
                        subProject = parentProject))
                try {
                  val result = currentTask.execute(environment, projectContext, args)
                  logger.debug("Executed task '${currentTask.id}' with result: $result")
                  if (!result.success) {
                    val errorMessage = result.message ?: "Task failed without message"
                    logger.error(
                        "Exception during execution of task '${currentTask.id}' in project '$projectName': $errorMessage")
                    eventPublisher.publishEvent(
                        taskFailedEvent(
                            projectName,
                            executionId,
                            currentTask.id,
                            message = errorMessage,
                            errorDetails = errorMessage,
                            parentProject = parentProject,
                        ))
                  } else {
                    eventPublisher.publishEvent(
                        taskCompletedEvent(
                            projectName,
                            executionId,
                            currentTask.id,
                            message = result.message ?: "Task ${currentTask.id} completed successfully",
                            subProject = parentProject))
                  }
                  taskCache.store(currentTask.id, result)
                  return@map result
                } catch (e: Exception) {
                  val errorMessage = e.message ?: "Unknown error"
                  val stackTrace = e.stackTraceToString()
                  eventPublisher.publishEvent(
                      taskFailedEvent(
                          projectName,
                          executionId,
                          currentTask.id,
                          message = "Task '${currentTask.id}' failed with exception: $errorMessage",
                          errorDetails = "Exception: $errorMessage\n\nStack Trace:\n$stackTrace",
                          parentProject = parentProject,
                      ))
                  logger.error("Exception during execution of task '${currentTask.id}'", e)
                  return@map TaskResult.failure(
                      "Task '${currentTask.id}' failed with exception: $errorMessage")
                }
              }
              .map { it }
      val success = results.all { it.success }
      if (!success) {
        val failedTasks = results.filter { !it.success }
        val failedMessages = failedTasks.mapNotNull { it.message }.joinToString(", ")
        val errorMessage =
            "Execution failed. ${failedTasks.size} task(s) failed" +
                if (failedMessages.isNotEmpty()) ": $failedMessages" else ""
          return TaskResult.failure(errorMessage)
      } else {
        return TaskResult.success("All tasks completed successfully")
      }
    } catch (e: Exception) {
      val errorMessage = e.message ?: "Unknown error"
      val stackTrace = e.stackTraceToString()
        return TaskResult.failure("Execution failed with exception: $errorMessage\n\nStack Trace:\n$stackTrace")
    }
  }
}
