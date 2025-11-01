package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.engine.core.project.app.ProjectService
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionId
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

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
    private val eventCollector: ExecutionEventCollector
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
   * in multi-project builds. If any subproject fails, the entire execution fails.
   *
   * @param projectName The name of the project
   * @param taskId The unique identifier of the task to execute
   * @param args List of arguments to pass to the task
   * @return The execution ID for tracking the task execution
   * @throws IllegalArgumentException if the project or task is not found
   * @throws RuntimeException if execution fails in any project or subproject
   */
  fun executeTask(projectName: String, taskId: String, args: List<String>): ExecutionId {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")

    fun executeRecursivelyOverSubprojectsFirst(
        project: Project,
        args: List<String>,
        parentProject: String? = null
    ): ExecutionId {
      // Execute in all subprojects first
      project.subProjects.forEach { subProject ->
        try {
          executeRecursivelyOverSubprojectsFirst(subProject, args, project.name)
        } catch (e: RuntimeException) {
          // Subproject failed, propagate the failure
          throw RuntimeException("Subproject ${subProject.name} failed: ${e.message}", e)
        }
      }
      
      val task =
          project.taskRegistry.all().firstOrNull { it.id == taskId }
              ?: throw IllegalArgumentException("Task not found")
      
      // This will throw RuntimeException if execution fails
      return executor.execute(project, task, project.context, args, parentProject)
    }

    return executeRecursivelyOverSubprojectsFirst(project, args)
  }

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
