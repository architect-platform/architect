package io.github.architectplatform.cloud.repository

import io.github.architectplatform.cloud.domain.Project
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface ProjectRepository : CrudRepository<Project, String> {
    fun findByEngineId(engineId: String): List<Project>
    fun findByName(name: String): Project?
}
