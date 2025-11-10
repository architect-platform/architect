package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus
import io.github.architectplatform.data.application.ports.inbound.ManageEngineUseCase
import io.github.architectplatform.data.application.ports.outbound.EngineInstancePort
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
        
        // Check if engine already exists
        val existing = enginePort.findById(id)
        if (existing != null) {
            logger.info("Engine $id already registered, updating...")
            val updated = existing.reactivate()
            val saved = enginePort.save(updated)
            eventBroadcast.broadcastEngineRegistered(saved)
            return saved
        }
        
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
    
    override fun markEngineInactive(engineId: String) {
        logger.info("Marking engine $engineId as inactive")
        enginePort.updateStatus(engineId, EngineStatus.INACTIVE)
    }
    
    override fun markEngineOffline(engineId: String) {
        logger.info("Marking engine $engineId as offline")
        enginePort.updateStatus(engineId, EngineStatus.OFFLINE)
    }
}
