package io.github.architectplatform.cloud.adapters.outbound.persistence.repositories

import io.github.architectplatform.cloud.adapters.outbound.persistence.entities.ExecutionEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface JdbcExecutionRepository : CrudRepository<ExecutionEntity, String> {
    fun findByProjectId(projectId: String): List<ExecutionEntity>
    fun findByEngineId(engineId: String): List<ExecutionEntity>
    fun findByStatus(status: String): List<ExecutionEntity>
}
