package io.github.architectplatform.data.adapters.outbound.persistence.repositories

import io.github.architectplatform.data.adapters.outbound.persistence.entities.ProjectEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

/**
 * Micronaut Data JDBC repository for projects.
 */
@JdbcRepository(dialect = Dialect.H2)
interface JdbcProjectRepository : CrudRepository<ProjectEntity, String> {
    fun findByEngineId(engineId: String): List<ProjectEntity>
}
