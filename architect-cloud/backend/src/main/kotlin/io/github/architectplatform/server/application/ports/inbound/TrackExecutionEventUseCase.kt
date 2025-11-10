package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.ExecutionEvent

/**
 * Inbound port for execution event tracking use cases.
 * Defines operations that can be performed on execution events from outside the application core.
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
    
    fun getExecutionEvents(executionId: String): List<ExecutionEvent>
}
