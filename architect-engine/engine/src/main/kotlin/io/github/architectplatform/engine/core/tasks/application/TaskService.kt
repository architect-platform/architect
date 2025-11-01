package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.project.app.ProjectService
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents.executionCompletedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents.executionFailedEvent
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents.executionStartedEvent
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.github.architectplatform.engine.domain.events.generateExecutionId
import io.micronaut.context.event.ApplicationEventPublisher
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Service responsible for task management and execution within projects.
 *
 * This service provides functionality to:
 * - Discover and retrieve tasks available in registered projects
 * - Execute tasks with custom arguments
 * - Stream execution events in real-time
 * - Handle recursive task execution across project hierarchies
 *
 * @property projectService Service for project management
 * @property executor Executor for running tasks
 * @property eventCollector Collector for execution event streams
 */
@Singleton
class TaskService(
    private val projectService: ProjectService,
    private val executor: TaskExecutor,
    private val eventCollector: ExecutionEventCollector,
    private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>
) {

  /**
   * Retrieves all available tasks for a project.
   *
   * @param projectName The name of the project
   * @return List of tasks sorted by their identifiers
   * @throws IllegalArgumentException if the project is not found
   */
  fun getAllTasks(projectName: String): List<Task> {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")
    return project.taskRegistry.all().sortedBy { it.id }
  }

  /**
   * Retrieves a specific task by its identifier.
   *
   * @param projectName The name of the project
   * @param taskId The unique identifier of the task
   * @return The task instance
   * @throws IllegalArgumentException if the project or task is not found
   */
  fun getTaskById(projectName: String, taskId: String): Task {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")
    return project.taskRegistry.get(taskId) ?: throw IllegalArgumentException("Task not found")
  }

  /**
   * Executes a task within a project hierarchy.
   *
   * The task is executed recursively across all subprojects first (depth-first),
   * then executed in the parent project. This ensures proper dependency ordering
   * in multi-project builds. All subprojects share the same executionId for unified
   * event tracking.
   *
   * @param projectName The name of the project
   * @param taskId The unique identifier of the task to execute
   * @param args List of arguments to pass to the task
   * @return The execution ID for tracking the task execution
   * @throws IllegalArgumentException if the project or task is not found
   */
  fun executeTask(projectName: String, taskId: String, args: List<String>): ExecutionId {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")

    // Generate a single execution ID for the entire execution tree
    val executionId = generateExecutionId()
      CoroutineScope(IO).launch {
          eventPublisher.publishEvent(
              executionStartedEvent(
                  projectName,
                  executionId,
                  message = "Starting execution of task: $taskId in project: $projectName")
          )
        val result = executeRecursivelyOverSubprojectsFirst(project, taskId, args, executionId = executionId)
        if (!result.success) {
            eventPublisher.publishEvent(
                executionFailedEvent(
                    projectName,
                    executionId,
                    message = "Execution failed: $result.",
                    errorDetails = "${result.message}",
                )
            )
        } else {
            eventPublisher.publishEvent(
                executionCompletedEvent(
                    projectName,
                    executionId,
                    message = "All tasks completed successfully")
            )
        }
      }

    return executionId
  }

    private suspend fun executeRecursivelyOverSubprojectsFirst(
        project: Project,
        taskId: String,
        args: List<String>,
        parentProject: String? = null,
        executionId: ExecutionId = generateExecutionId(),
    ): TaskResult {
        // Execute all subprojects concurrently and wait for results
        val subResults = project.subProjects.map { subProject ->
            CoroutineScope(IO).async {
                executeRecursivelyOverSubprojectsFirst(subProject, taskId, args, project.name, executionId)
            }
        }.awaitAll()

        // If any subproject failed, propagate failure
        if (subResults.any { !it.success }) {
            return TaskResult.failure("Some subprojects failed", subResults)
        }

        // Execute task in the current project only if present
        val task = project.taskRegistry.all().firstOrNull { it.id == taskId }
        if (task == null) {
            return TaskResult.success("Task $taskId not found in project ${project.name}, skipping execution.")
        }

        // Execute using shared executionId
        val (_, deferredResult) =
            executor.execute(project, task, project.context, args, parentProject, executionId)

        return deferredResult.await()
    }

  private fun generateExecutionId(): ExecutionId = UUID.randomUUID().toString()

  /**
   * Returns a flow of execution events for a specific task execution.
   *
   * This provides real-time streaming of events such as task start, progress,
   * completion, and failures during task execution.
   *
   * @param executionId The unique identifier of the execution
   * @return Flow of execution events
   */
  fun getExecutionFlow(executionId: ExecutionId): Flow<ArchitectEvent<ExecutionEvent>> {
    return eventCollector.getFlow(executionId)
  }
}
