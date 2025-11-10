package io.github.architectplatform.server.application.ports.outbound

import io.github.architectplatform.server.application.domain.EngineInstance
import io.github.architectplatform.server.application.domain.EngineStatus

/**
 * Outbound port for engine instance persistence operations.
 * Defines the contract for engine repository implementations.
 */
interface EngineInstancePort {
    fun save(engine: EngineInstance): EngineInstance
    fun findById(id: String): EngineInstance?
    fun findAll(): List<EngineInstance>
    fun findByStatus(status: EngineStatus): List<EngineInstance>
    fun updateHeartbeat(id: String): Boolean
}
