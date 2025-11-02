package io.github.architectplatform.cloud.controller

import io.github.architectplatform.cloud.domain.Project
import io.github.architectplatform.cloud.dto.RegisterProjectRequest
import io.github.architectplatform.cloud.service.CloudService
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/api/projects")
@ExecuteOn(TaskExecutors.IO)
class ProjectController(private val cloudService: CloudService) {
    
    @Post
    fun registerProject(@Body request: RegisterProjectRequest): Project {
        return cloudService.registerProject(request)
    }
    
    @Get("/{projectId}")
    fun getProject(@PathVariable projectId: String): Project? {
        return cloudService.getProject(projectId)
    }
    
    @Get
    fun getAllProjects(): List<Project> {
        return cloudService.getAllProjects()
    }
    
    @Get("/engine/{engineId}")
    fun getProjectsByEngine(@PathVariable engineId: String): List<Project> {
        return cloudService.getProjectsByEngine(engineId)
    }
}
