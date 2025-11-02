package io.github.architectplatform.cloud.adapters.outbound.persistence.repositories

import io.github.architectplatform.cloud.adapters.outbound.persistence.entities.ProjectEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface JdbcProjectRepository : CrudRepository<ProjectEntity, String> {
    fun findByEngineId(engineId: String): List<ProjectEntity>
    fun findByName(name: String): ProjectEntity?
}
