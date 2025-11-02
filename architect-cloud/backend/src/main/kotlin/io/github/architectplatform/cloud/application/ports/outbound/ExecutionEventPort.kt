package io.github.architectplatform.cloud.application.ports.outbound

import io.github.architectplatform.cloud.application.domain.ExecutionEvent

/**
 * Outbound port for execution event persistence operations.
 * Defines the contract for execution event repository implementations.
 */
interface ExecutionEventPort {
    fun save(event: ExecutionEvent): ExecutionEvent
    fun findByExecutionId(executionId: String): List<ExecutionEvent>
}
