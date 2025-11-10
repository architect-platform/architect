package io.github.architectplatform.data.application.ports.inbound

import io.github.architectplatform.data.application.domain.EngineInstance

/**
 * Inbound port for engine management use cases.
 * Defines operations for tracking and managing engine instances.
 */
interface ManageEngineUseCase {
    fun registerEngine(id: String, hostname: String, port: Int, version: String?): EngineInstance
    fun recordHeartbeat(engineId: String)
    fun getEngine(engineId: String): EngineInstance?
    fun getAllEngines(): List<EngineInstance>
    fun getActiveEngines(): List<EngineInstance>
    fun markEngineInactive(engineId: String)
    fun markEngineOffline(engineId: String)
}
