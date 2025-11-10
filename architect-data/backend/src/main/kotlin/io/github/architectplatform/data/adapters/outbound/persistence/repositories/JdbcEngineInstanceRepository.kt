package io.github.architectplatform.data.adapters.outbound.persistence.repositories

import io.github.architectplatform.data.adapters.outbound.persistence.entities.EngineInstanceEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

/**
 * Micronaut Data JDBC repository for engine instances.
 */
@JdbcRepository(dialect = Dialect.H2)
interface JdbcEngineInstanceRepository : CrudRepository<EngineInstanceEntity, String> {
    fun findByStatus(status: String): List<EngineInstanceEntity>
}
