package io.github.architectplatform.cli.client

import io.github.architectplatform.cli.dto.AuthResponse
import io.github.architectplatform.cli.dto.AuthStatusResponse
import io.github.architectplatform.cli.dto.ProjectDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.dto.SetGitHubTokenRequest
import io.github.architectplatform.cli.dto.TaskDTO
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
 * - Authentication management
 */
@Client("engine")
interface EngineCommandClient {

  /**
   * Retrieves all registered projects.
   *
   * @return List of all projects
   */
  @Get("/api/projects") fun getAllProjects(): List<ProjectDTO>

  /**
   * Registers a new project with the engine.
   *
   * @param request Project registration details
   * @return The registered project
   */
  @Post("/api/projects") fun registerProject(@Body request: RegisterProjectRequest): ProjectDTO

  /**
   * Retrieves a specific project by name.
   *
   * @param name The name of the project
   * @return The project, or null if not found
   */
  @Get("/api/projects/{name}") fun getProject(@PathVariable name: String): ProjectDTO?

  /**
   * Retrieves all tasks available for a project.
   *
   * @param projectName The name of the project
   * @return List of available tasks
   */
  @Get("/api/projects/{projectName}/tasks")
  fun getAllTasks(@PathVariable projectName: String): List<TaskDTO>

  /**
   * Retrieves a specific task for a project.
   *
   * @param projectName The name of the project
   * @param taskName The name of the task
   * @return The task, or null if not found
   */
  @Get("/api/projects/{projectName}/tasks/{taskName}")
  fun getTask(@PathVariable projectName: String, @PathVariable taskName: String): TaskDTO?

  /**
   * Executes a task within a project.
   *
   * @param projectName The name of the project
   * @param taskName The name of the task to execute
   * @param args Arguments to pass to the task
   * @return Execution identifier for monitoring progress
   */
  @Post("/api/projects/{projectName}/tasks/{taskName}")
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
  @Get("/api/executions/{executionId}")
  fun getExecutionFlow(@PathVariable executionId: ExecutionId): Flow<Map<String, Any>>
  
  // Authentication endpoints
  
  /**
   * Sets the GitHub token for authenticated API requests.
   *
   * @param request The request containing the GitHub token
   * @return Response indicating success or failure
   */
  @Post("/auth/github")
  fun setGitHubToken(@Body request: SetGitHubTokenRequest): AuthResponse
  
  /**
   * Checks the authentication status for GitHub.
   *
   * @return Status indicating if authenticated
   */
  @Get("/auth/github/status")
  fun getGitHubStatus(): AuthStatusResponse
  
  /**
   * Removes the stored GitHub token.
   *
   * @return Response indicating success or failure
   */
  @Delete("/auth/github")
  fun clearGitHubToken(): AuthResponse
}
