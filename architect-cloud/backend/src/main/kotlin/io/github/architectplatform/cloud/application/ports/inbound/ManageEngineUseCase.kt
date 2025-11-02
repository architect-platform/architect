package io.github.architectplatform.cloud.application.ports.inbound

import io.github.architectplatform.cloud.application.domain.EngineInstance

/**
 * Inbound port for engine management use cases.
 * Defines operations that can be performed on engines from outside the application core.
 */
interface ManageEngineUseCase {
    fun registerEngine(id: String, hostname: String, port: Int, version: String?): EngineInstance
    fun recordHeartbeat(engineId: String)
    fun getEngine(engineId: String): EngineInstance?
    fun getAllEngines(): List<EngineInstance>
    fun getActiveEngines(): List<EngineInstance>
}
