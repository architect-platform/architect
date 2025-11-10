package io.github.architectplatform.data.application.ports.outbound

import io.github.architectplatform.data.application.domain.Execution

/**
 * Outbound port for execution persistence.
 * Repository interface for execution data access.
 */
interface ExecutionPort {
    fun save(execution: Execution): Execution
    fun findById(id: String): Execution?
    fun findByProjectId(projectId: String): List<Execution>
    fun findByEngineId(engineId: String): List<Execution>
    fun findRecent(limit: Int): List<Execution>
}
