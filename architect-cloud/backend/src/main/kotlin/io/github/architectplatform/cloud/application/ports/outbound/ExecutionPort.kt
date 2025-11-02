package io.github.architectplatform.cloud.application.ports.outbound

import io.github.architectplatform.cloud.application.domain.Execution
import io.github.architectplatform.cloud.application.domain.ExecutionStatus

/**
 * Outbound port for execution persistence operations.
 * Defines the contract for execution repository implementations.
 */
interface ExecutionPort {
    fun save(execution: Execution): Execution
    fun findById(id: String): Execution?
    fun findByProjectId(projectId: String): List<Execution>
    fun findByEngineId(engineId: String): List<Execution>
    fun findByStatus(status: ExecutionStatus): List<Execution>
}
