package io.github.architectplatform.data.application.ports.inbound

import io.github.architectplatform.data.application.domain.Project

/**
 * Inbound port for project management use cases.
 * Defines operations for tracking projects across engines.
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
