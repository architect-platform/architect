package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.Execution
import io.github.architectplatform.data.application.domain.ExecutionStatus
import io.github.architectplatform.data.application.ports.inbound.TrackExecutionUseCase
import io.github.architectplatform.data.application.ports.outbound.ExecutionPort
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
        
        // Check if execution already exists and update it
        val existing = executionPort.findById(id)
        val execution = if (existing != null) {
            existing.updateStatus(status, message)
        } else {
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
        
        // Broadcast event based on status
        when (status) {
            ExecutionStatus.STARTED -> eventBroadcast.broadcastExecutionStarted(saved)
            ExecutionStatus.COMPLETED -> eventBroadcast.broadcastExecutionCompleted(saved)
            ExecutionStatus.FAILED -> eventBroadcast.broadcastExecutionFailed(saved)
            else -> eventBroadcast.broadcastExecutionUpdated(saved)
        }
        
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
    
    override fun getRecentExecutions(limit: Int): List<Execution> {
        return executionPort.findRecent(limit)
    }
}
