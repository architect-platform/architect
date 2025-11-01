package io.github.architectplatform.cli.client

import io.github.architectplatform.cli.dto.ProjectDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.dto.TaskDTO
import io.micronaut.http.annotation.Body
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
}
