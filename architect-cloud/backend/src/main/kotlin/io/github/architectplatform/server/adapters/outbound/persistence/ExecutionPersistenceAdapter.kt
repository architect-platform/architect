package io.github.architectplatform.server.adapters.outbound.persistence

import io.github.architectplatform.server.adapters.outbound.persistence.entities.ExecutionEntity
import io.github.architectplatform.server.adapters.outbound.persistence.repositories.JdbcExecutionRepository
import io.github.architectplatform.server.application.domain.Execution
import io.github.architectplatform.server.application.domain.ExecutionStatus
import io.github.architectplatform.server.application.ports.outbound.ExecutionPort
import jakarta.inject.Singleton

/**
 * Persistence adapter for executions.
 * Implements the outbound port using JDBC repository.
 */
@Singleton
class ExecutionPersistenceAdapter(
    private val repository: JdbcExecutionRepository
) : ExecutionPort {
    
    override fun save(execution: Execution): Execution {
        val entity = ExecutionEntity.fromDomain(execution)
        val saved = repository.save(entity)
        return saved.toDomain()
    }
    
    override fun findById(id: String): Execution? {
        return repository.findById(id).map { it.toDomain() }.orElse(null)
    }
    
    override fun findByProjectId(projectId: String): List<Execution> {
        return repository.findByProjectId(projectId).map { it.toDomain() }
    }
    
    override fun findByEngineId(engineId: String): List<Execution> {
        return repository.findByEngineId(engineId).map { it.toDomain() }
    }
    
    override fun findByStatus(status: ExecutionStatus): List<Execution> {
        return repository.findByStatus(status.name).map { it.toDomain() }
    }
}
