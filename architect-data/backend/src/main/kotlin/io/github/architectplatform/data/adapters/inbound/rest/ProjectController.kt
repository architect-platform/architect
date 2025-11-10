package io.github.architectplatform.data.adapters.inbound.rest

import io.github.architectplatform.data.adapters.inbound.rest.dto.ProjectResponse
import io.github.architectplatform.data.adapters.inbound.rest.dto.RegisterProjectRequest
import io.github.architectplatform.data.application.ports.inbound.ManageProjectUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * REST controller for project management.
 * Inbound adapter that exposes project use cases via HTTP.
 */
@Controller("/api/projects")
@ExecuteOn(TaskExecutors.IO)
class ProjectController(
    private val manageProjectUseCase: ManageProjectUseCase
) {
    
    @Post
    fun registerProject(@Body request: RegisterProjectRequest): ProjectResponse {
        val project = manageProjectUseCase.registerProject(
            id = request.id,
            name = request.name,
            path = request.path,
            engineId = request.engineId,
            description = request.description
        )
        return ProjectResponse.fromDomain(project)
    }
    
    @Get("/{id}")
    fun getProject(@PathVariable id: String): ProjectResponse? {
        return manageProjectUseCase.getProject(id)?.let { ProjectResponse.fromDomain(it) }
    }
    
    @Get
    fun getAllProjects(): List<ProjectResponse> {
        return manageProjectUseCase.getAllProjects().map { ProjectResponse.fromDomain(it) }
    }
    
    @Get("/engine/{engineId}")
    fun getProjectsByEngine(@PathVariable engineId: String): List<ProjectResponse> {
        return manageProjectUseCase.getProjectsByEngine(engineId).map { ProjectResponse.fromDomain(it) }
    }
}
