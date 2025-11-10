package io.github.architectplatform.data.adapters.outbound.persistence

import io.github.architectplatform.data.adapters.outbound.persistence.entities.EngineInstanceEntity
import io.github.architectplatform.data.adapters.outbound.persistence.repositories.JdbcEngineInstanceRepository
import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus
import io.github.architectplatform.data.application.ports.outbound.EngineInstancePort
import jakarta.inject.Singleton
import java.time.Instant

/**
 * Persistence adapter for engine instances.
 * Implements the outbound port using JDBC.
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
        return repository.findById(id).orElse(null)?.toDomain()
    }
    
    override fun findAll(): List<EngineInstance> {
        return repository.findAll().map { it.toDomain() }
    }
    
    override fun findByStatus(status: EngineStatus): List<EngineInstance> {
        return repository.findByStatus(status.name).map { it.toDomain() }
    }
    
    override fun updateHeartbeat(engineId: String) {
        repository.findById(engineId).ifPresent { engine ->
            repository.update(engine.copy(lastHeartbeat = Instant.now()))
        }
    }
    
    override fun updateStatus(engineId: String, status: EngineStatus) {
        repository.findById(engineId).ifPresent { engine ->
            repository.update(engine.copy(status = status.name))
        }
    }
}
