package io.github.architectplatform.data.adapters.outbound.persistence.repositories

import io.github.architectplatform.data.adapters.outbound.persistence.entities.ExecutionEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

/**
 * Micronaut Data JDBC repository for executions.
 */
@JdbcRepository(dialect = Dialect.H2)
interface JdbcExecutionRepository : CrudRepository<ExecutionEntity, String> {
    fun findByProjectId(projectId: String): List<ExecutionEntity>
    fun findByEngineId(engineId: String): List<ExecutionEntity>
    
    @Query("SELECT * FROM executions ORDER BY started_at DESC LIMIT :limit")
    fun findRecent(limit: Int): List<ExecutionEntity>
}
