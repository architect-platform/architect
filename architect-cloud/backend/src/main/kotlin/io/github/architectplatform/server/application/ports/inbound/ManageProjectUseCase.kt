package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.Project

/**
 * Inbound port for project management use cases.
 * Defines operations that can be performed on projects from outside the application core.
 */
interface ManageProjectUseCase {
    fun registerProject(
        id: String,
        name: String,
        path: String,
        engineId: String,
        description: String? = null
    ): Project
    
    fun getProject(projectId: String): Project?
    fun getAllProjects(): List<Project>
    fun getProjectsByEngine(engineId: String): List<Project>
}
