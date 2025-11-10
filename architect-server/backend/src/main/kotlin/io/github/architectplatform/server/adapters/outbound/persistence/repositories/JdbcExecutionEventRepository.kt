package io.github.architectplatform.server.adapters.outbound.persistence.repositories

import io.github.architectplatform.server.adapters.outbound.persistence.entities.ExecutionEventEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface JdbcExecutionEventRepository : CrudRepository<ExecutionEventEntity, String> {
    fun findByExecutionId(executionId: String): List<ExecutionEventEntity>
}
