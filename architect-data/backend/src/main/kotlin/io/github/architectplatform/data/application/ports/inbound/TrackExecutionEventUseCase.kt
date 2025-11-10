package io.github.architectplatform.data.application.ports.inbound

import io.github.architectplatform.data.application.domain.ExecutionEvent

/**
 * Inbound port for execution event tracking use cases.
 * Defines operations for monitoring detailed execution events.
 */
interface TrackExecutionEventUseCase {
    fun reportEvent(
        id: String,
        executionId: String,
        eventType: String,
        taskId: String? = null,
        message: String? = null,
        output: String? = null,
        success: Boolean = true
    ): ExecutionEvent
    
    fun getEventsByExecution(executionId: String): List<ExecutionEvent>
}
