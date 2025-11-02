package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.EngineInstance
import io.github.architectplatform.cloud.application.domain.EngineStatus
import io.github.architectplatform.cloud.application.ports.inbound.ManageEngineUseCase
import io.github.architectplatform.cloud.application.ports.outbound.EngineInstancePort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Application service implementing engine management use cases.
 * Contains business logic for engine operations.
 */
@Singleton
class EngineService(
    private val enginePort: EngineInstancePort,
    private val eventBroadcast: EventBroadcastService
) : ManageEngineUseCase {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun registerEngine(id: String, hostname: String, port: Int, version: String?): EngineInstance {
        logger.info("Registering engine: $id at $hostname:$port")
        
        val engine = EngineInstance(
            id = id,
            hostname = hostname,
            port = port,
            version = version,
            status = EngineStatus.ACTIVE
        )
        
        val saved = enginePort.save(engine)
        eventBroadcast.broadcastEngineRegistered(saved)
        return saved
    }
    
    override fun recordHeartbeat(engineId: String) {
        logger.debug("Recording heartbeat for engine: $engineId")
        enginePort.updateHeartbeat(engineId)
        eventBroadcast.broadcastEngineHeartbeat(engineId)
    }
    
    override fun getEngine(engineId: String): EngineInstance? {
        return enginePort.findById(engineId)
    }
    
    override fun getAllEngines(): List<EngineInstance> {
        return enginePort.findAll()
    }
    
    override fun getActiveEngines(): List<EngineInstance> {
        return enginePort.findByStatus(EngineStatus.ACTIVE)
    }
}
