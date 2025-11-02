package io.github.architectplatform.cloud.adapters.outbound.persistence

import io.github.architectplatform.cloud.adapters.outbound.persistence.entities.EngineInstanceEntity
import io.github.architectplatform.cloud.adapters.outbound.persistence.repositories.JdbcEngineInstanceRepository
import io.github.architectplatform.cloud.application.domain.EngineInstance
import io.github.architectplatform.cloud.application.domain.EngineStatus
import io.github.architectplatform.cloud.application.ports.outbound.EngineInstancePort
import jakarta.inject.Singleton
import java.time.Instant

/**
 * Persistence adapter for engine instances.
 * Implements the outbound port using JDBC repository.
 */
@Singleton
class EngineInstancePersistenceAdapter(
    private val repository: JdbcEngineInstanceRepository
) : EngineInstancePort {
    
    override fun save(engine: EngineInstance): EngineInstance {
        val entity = EngineInstanceEntity.fromDomain(engine)
        val saved = repository.save(entity)
        return saved.toDomain()
    }
    
    override fun findById(id: String): EngineInstance? {
        return repository.findById(id).map { it.toDomain() }.orElse(null)
    }
    
    override fun findAll(): List<EngineInstance> {
        return repository.findAll().map { it.toDomain() }
    }
    
    override fun findByStatus(status: EngineStatus): List<EngineInstance> {
        return repository.findByStatus(status.name).map { it.toDomain() }
    }
    
    override fun updateHeartbeat(id: String): Boolean {
        val updated = repository.updateLastHeartbeat(id, Instant.now())
        return updated > 0
    }
}
