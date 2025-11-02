package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.Execution
import io.github.architectplatform.cloud.application.domain.ExecutionStatus
import io.github.architectplatform.cloud.application.ports.inbound.TrackExecutionUseCase
import io.github.architectplatform.cloud.application.ports.outbound.ExecutionPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Application service implementing execution tracking use cases.
 * Contains business logic for execution operations.
 */
@Singleton
class ExecutionService(
    private val executionPort: ExecutionPort,
    private val eventBroadcast: EventBroadcastService
) : TrackExecutionUseCase {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun reportExecution(
        id: String,
        projectId: String,
        engineId: String,
        taskId: String,
        status: ExecutionStatus,
        message: String?,
        errorDetails: String?
    ): Execution {
        logger.info("Reporting execution: $id for task: $taskId with status: $status")
        
        val existingExecution = executionPort.findById(id)
        
        val execution = if (existingExecution != null) {
            // Update existing execution
            existingExecution.updateStatus(status, message).let {
                if (errorDetails != null) {
                    it.copy(errorDetails = errorDetails)
                } else {
                    it
                }
            }
        } else {
            // Create new execution
            Execution(
                id = id,
                projectId = projectId,
                engineId = engineId,
                taskId = taskId,
                status = status,
                message = message,
                errorDetails = errorDetails
            )
        }
        
        val saved = executionPort.save(execution)
        eventBroadcast.broadcastExecutionReported(saved)
        return saved
    }
    
    override fun getExecution(executionId: String): Execution? {
        return executionPort.findById(executionId)
    }
    
    override fun getExecutionsByProject(projectId: String): List<Execution> {
        return executionPort.findByProjectId(projectId)
    }
    
    override fun getExecutionsByEngine(engineId: String): List<Execution> {
        return executionPort.findByEngineId(engineId)
    }
}
