package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.ExecutionEvent
import io.github.architectplatform.cloud.application.ports.inbound.TrackExecutionEventUseCase
import io.github.architectplatform.cloud.application.ports.outbound.ExecutionEventPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Application service implementing execution event tracking use cases.
 * Contains business logic for execution event operations.
 */
@Singleton
class ExecutionEventService(
    private val eventPort: ExecutionEventPort,
    private val eventBroadcast: EventBroadcastService
) : TrackExecutionEventUseCase {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun reportEvent(
        id: String,
        executionId: String,
        eventType: String,
        taskId: String?,
        message: String?,
        output: String?,
        success: Boolean
    ): ExecutionEvent {
        logger.debug("Reporting event: $eventType for execution: $executionId")
        
        val event = ExecutionEvent(
            id = id,
            executionId = executionId,
            eventType = eventType,
            taskId = taskId,
            message = message,
            output = output,
            success = success
        )
        
        val saved = eventPort.save(event)
        eventBroadcast.broadcastExecutionEvent(saved)
        return saved
    }
    
    override fun getExecutionEvents(executionId: String): List<ExecutionEvent> {
        return eventPort.findByExecutionId(executionId)
    }
}
