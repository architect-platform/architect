package io.github.architectplatform.cli.client

import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.ProjectDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.dto.TaskDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskStatsDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import kotlinx.coroutines.flow.Flow

/**
 * HTTP client for communicating with the Architect Engine API.
 *
 * Provides methods for:
 * - Project management (register, list, get)
 * - Task management (list, get)
 * - Task execution
 * - Execution monitoring via reactive streams
 */
@Client("engine", path = "/api")
interface EngineCommandClient {

  /**
   * Retrieves all registered projects.
   *
   * @return List of all projects
   */
  @Get("/projects") fun getAllProjects(): List<ProjectDTO>

  /**
   * Registers a new project with the engine.
   *
   * @param request Project registration details
   * @return The registered project
   */
  @Post("/projects") fun registerProject(@Body request: RegisterProjectRequest): ProjectDTO

  /**
   * Retrieves a specific project by name.
   *
   * @param name The name of the project
   * @return The project, or null if not found
   */
  @Get("/projects/{name}") fun getProject(@PathVariable name: String): ProjectDTO?

  /**
   * Retrieves all tasks available for a project.
   *
   * @param projectName The name of the project
   * @return List of available tasks
   */
  @Get("/projects/{projectName}/tasks")
  fun getAllTasks(@PathVariable projectName: String): List<TaskDTO>

  /**
   * Retrieves a specific task for a project.
   *
   * @param projectName The name of the project
   * @param taskName The name of the task
   * @return The task, or null if not found
   */
  @Get("/projects/{projectName}/tasks/{taskName}")
  fun getTask(@PathVariable projectName: String, @PathVariable taskName: String): TaskDTO?

  /**
   * Returns the execution plan for a task without running it.
   *
   * @param projectName The name of the project
   * @param taskName The name of the task
   * @return The ordered execution plan with batch assignments
   */
  @Get("/projects/{projectName}/tasks/{taskName}/plan")
  fun planTask(@PathVariable projectName: String, @PathVariable taskName: String): TaskPlanDTO

  /**
   * Executes a task within a project.
   *
   * @param projectName The name of the project
   * @param taskName The name of the task to execute
   * @param args Arguments to pass to the task
   * @return Execution identifier for monitoring progress
   */
  @Post("/projects/{projectName}/tasks/{taskName}")
  fun execute(
      @PathVariable projectName: String,
      @PathVariable taskName: String,
      @Body args: List<String>
  ): ExecutionId

  /**
   * Retrieves a reactive flow of execution events.
   *
   * @param executionId The execution identifier
   * @return Flow of event maps emitted during task execution
   */
  @Get("/executions/{executionId}")
  fun getExecutionFlow(@PathVariable executionId: ExecutionId): Flow<Map<String, Any>>

  /**
   * Retrieves recent execution history across all projects.
   *
   * @return List of execution records, newest first
   */
  @Get("/history")
  fun getHistory(): List<HistoryRecordDTO>

  /**
   * Retrieves recent execution history for a specific project.
   *
   * @param project The project name
   * @return List of execution records for this project, newest first
   */
  @Get("/history/{project}")
  fun getProjectHistory(@PathVariable project: String): List<HistoryRecordDTO>

  /**
   * Validates the configuration of a registered project.
   *
   * @param projectName The project name
   * @return Validation result with errors and warnings
   */
  @Get("/projects/{projectName}/validate")
  fun validateProject(@PathVariable projectName: String): ValidationResultDTO

  @Post("/projects/{projectName}/reload-plugins")
  fun reloadProjectPlugins(@PathVariable projectName: String): ProjectDTO

  /**
   * Cancels a running execution.
   *
   * @param executionId The execution identifier to cancel
   * @return Map with executionId and cancelled status
   */
  @Delete("/executions/{executionId}")
  fun cancelExecution(@PathVariable executionId: ExecutionId): Map<String, Any>

  /**
   * Returns performance statistics for a specific task.
   *
   * @param project Project name
   * @param taskId  Task identifier
   * @return Statistics or null if no history found
   */
  @Get("/projects/{project}/tasks/{taskId}/stats")
  fun getTaskStats(@PathVariable project: String, @PathVariable taskId: String): TaskStatsDTO?

  /**
   * Returns performance statistics for every task with history in the project.
   *
   * @param project Project name
   * @return List of per-task statistics
   */
  @Get("/projects/{project}/tasks/stats")
  fun getAllTaskStats(@PathVariable project: String): List<TaskStatsDTO>
}
