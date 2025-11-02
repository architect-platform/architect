package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.Project
import io.github.architectplatform.cloud.application.ports.inbound.ManageProjectUseCase
import io.github.architectplatform.cloud.application.ports.outbound.ProjectPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Application service implementing project management use cases.
 * Contains business logic for project operations.
 */
@Singleton
class ProjectService(
    private val projectPort: ProjectPort,
    private val eventBroadcast: EventBroadcastService
) : ManageProjectUseCase {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun registerProject(
        id: String,
        name: String,
        path: String,
        engineId: String,
        description: String?
    ): Project {
        logger.info("Registering project: $name for engine: $engineId")
        
        val project = Project(
            id = id,
            name = name,
            path = path,
            engineId = engineId,
            description = description
        )
        
        val saved = projectPort.save(project)
        eventBroadcast.broadcastProjectRegistered(saved)
        return saved
    }
    
    override fun getProject(projectId: String): Project? {
        return projectPort.findById(projectId)
    }
    
    override fun getAllProjects(): List<Project> {
        return projectPort.findAll()
    }
    
    override fun getProjectsByEngine(engineId: String): List<Project> {
        return projectPort.findByEngineId(engineId)
    }
}
