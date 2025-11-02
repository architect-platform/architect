package io.github.architectplatform.cloud.service

import io.github.architectplatform.cloud.domain.*
import io.github.architectplatform.cloud.dto.*
import io.github.architectplatform.cloud.repository.*
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

@Singleton
class CloudService(
    private val engineInstanceRepository: EngineInstanceRepository,
    private val projectRepository: ProjectRepository,
    private val executionRepository: ExecutionRepository,
    private val executionEventRepository: ExecutionEventRepository
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    // Engine Instance Management
    
    fun registerEngine(request: RegisterEngineRequest): EngineInstance {
        logger.info("Registering engine: ${request.id}")
        val engine = EngineInstance(
            id = request.id,
            hostname = request.hostname,
            port = request.port,
            version = request.version,
            status = EngineStatus.ACTIVE
        )
        return engineInstanceRepository.save(engine)
    }
    
    fun heartbeat(engineId: String) {
        logger.debug("Heartbeat from engine: $engineId")
        engineInstanceRepository.updateLastHeartbeat(engineId, Instant.now())
    }
    
    fun getEngine(engineId: String): EngineInstance? {
        return engineInstanceRepository.findById(engineId).orElse(null)
    }
    
    fun getAllEngines(): List<EngineInstance> {
        return engineInstanceRepository.findAll().toList()
    }
    
    fun getActiveEngines(): List<EngineInstance> {
        return engineInstanceRepository.findByStatus(EngineStatus.ACTIVE)
    }
    
    // Project Management
    
    fun registerProject(request: RegisterProjectRequest): Project {
        logger.info("Registering project: ${request.name} for engine: ${request.engineId}")
        val project = Project(
            id = request.id,
            name = request.name,
            path = request.path,
            engineId = request.engineId,
            description = request.description
        )
        return projectRepository.save(project)
    }
    
    fun getProject(projectId: String): Project? {
        return projectRepository.findById(projectId).orElse(null)
    }
    
    fun getAllProjects(): List<Project> {
        return projectRepository.findAll().toList()
    }
    
    fun getProjectsByEngine(engineId: String): List<Project> {
        return projectRepository.findByEngineId(engineId)
    }
    
    // Execution Management
    
    fun reportExecution(request: ReportExecutionRequest): Execution {
        logger.info("Reporting execution: ${request.id} for task: ${request.taskId}")
        
        val existingExecution = executionRepository.findById(request.id).orElse(null)
        
        val execution = if (existingExecution != null) {
            existingExecution.copy(
                status = ExecutionStatus.valueOf(request.status),
                message = request.message,
                errorDetails = request.errorDetails,
                completedAt = if (request.status in listOf("COMPLETED", "FAILED")) Instant.now() else null
            )
        } else {
            Execution(
                id = request.id,
                projectId = request.projectId,
                engineId = request.engineId,
                taskId = request.taskId,
                status = ExecutionStatus.valueOf(request.status),
                message = request.message,
                errorDetails = request.errorDetails
            )
        }
        
        return executionRepository.save(execution)
    }
    
    fun getExecution(executionId: String): Execution? {
        return executionRepository.findById(executionId).orElse(null)
    }
    
    fun getExecutionsByProject(projectId: String): List<Execution> {
        return executionRepository.findByProjectId(projectId)
    }
    
    fun getExecutionsByEngine(engineId: String): List<Execution> {
        return executionRepository.findByEngineId(engineId)
    }
    
    // Event Management
    
    fun reportEvent(request: ReportEventRequest): ExecutionEvent {
        logger.debug("Reporting event: ${request.eventType} for execution: ${request.executionId}")
        val event = ExecutionEvent(
            id = request.id,
            executionId = request.executionId,
            eventType = request.eventType,
            taskId = request.taskId,
            message = request.message,
            output = request.output,
            success = request.success
        )
        return executionEventRepository.save(event)
    }
    
    fun getExecutionEvents(executionId: String): List<ExecutionEvent> {
        return executionEventRepository.findByExecutionId(executionId)
    }
}
