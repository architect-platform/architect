package io.github.architectplatform.data.application.ports.outbound

import io.github.architectplatform.data.application.domain.ExecutionEvent

/**
 * Outbound port for execution event persistence.
 * Repository interface for execution event data access.
 */
interface ExecutionEventPort {
    fun save(event: ExecutionEvent): ExecutionEvent
    fun findByExecutionId(executionId: String): List<ExecutionEvent>
}
