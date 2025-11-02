package io.github.architectplatform.cloud.repository

import io.github.architectplatform.cloud.domain.Execution
import io.github.architectplatform.cloud.domain.ExecutionStatus
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface ExecutionRepository : CrudRepository<Execution, String> {
    fun findByProjectId(projectId: String): List<Execution>
    fun findByEngineId(engineId: String): List<Execution>
    fun findByStatus(status: ExecutionStatus): List<Execution>
}
