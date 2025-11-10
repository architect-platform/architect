package io.github.architectplatform.data.application.ports.outbound

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus

/**
 * Outbound port for engine instance persistence.
 * Repository interface for engine data access.
 */
interface EngineInstancePort {
    fun save(engine: EngineInstance): EngineInstance
    fun findById(id: String): EngineInstance?
    fun findAll(): List<EngineInstance>
    fun findByStatus(status: EngineStatus): List<EngineInstance>
    fun updateHeartbeat(engineId: String)
    fun updateStatus(engineId: String, status: EngineStatus)
}
