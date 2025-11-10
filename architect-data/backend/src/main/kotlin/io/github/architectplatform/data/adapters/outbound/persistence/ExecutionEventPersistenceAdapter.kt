package io.github.architectplatform.data.adapters.outbound.persistence

import io.github.architectplatform.data.adapters.outbound.persistence.entities.ExecutionEventEntity
import io.github.architectplatform.data.adapters.outbound.persistence.repositories.JdbcExecutionEventRepository
import io.github.architectplatform.data.application.domain.ExecutionEvent
import io.github.architectplatform.data.application.ports.outbound.ExecutionEventPort
import jakarta.inject.Singleton

/**
 * Persistence adapter for execution events.
 * Implements the outbound port using JDBC.
 */
@Singleton
class ExecutionEventPersistenceAdapter(
    private val repository: JdbcExecutionEventRepository
) : ExecutionEventPort {
    
    override fun save(event: ExecutionEvent): ExecutionEvent {
        val entity = ExecutionEventEntity.fromDomain(event)
        val saved = repository.save(entity)
        return saved.toDomain()
    }
    
    override fun findByExecutionId(executionId: String): List<ExecutionEvent> {
        return repository.findByExecutionId(executionId).map { it.toDomain() }
    }
}
