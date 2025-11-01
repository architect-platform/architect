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

@Client("engine", path = "/api")
interface EngineCommandClient {

  @Get("/projects") fun getAllProjects(): List<ProjectDTO>

  @Post("/projects") fun registerProject(@Body request: RegisterProjectRequest): ProjectDTO

  @Get("/projects/{name}") fun getProject(@PathVariable name: String): ProjectDTO?

  @Get("/projects/{projectName}/tasks")
  fun getAllTasks(@PathVariable projectName: String): List<TaskDTO>

  @Get("/projects/{projectName}/tasks/{taskName}")
  fun getTask(@PathVariable projectName: String, @PathVariable taskName: String): TaskDTO?

  @Post("/projects/{projectName}/tasks/{taskName}")
  fun execute(
      @PathVariable projectName: String,
      @PathVariable taskName: String,
      @Body args: List<String>
  ): ExecutionId

  @Get("/executions/{executionId}")
  fun getExecutionFlow(@PathVariable executionId: ExecutionId): Flow<Map<String, Any>>
}
