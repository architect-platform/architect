package io.github.architectplatform.cloud.repository

import io.github.architectplatform.cloud.domain.EngineInstance
import io.github.architectplatform.cloud.domain.EngineStatus
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.Instant

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface EngineInstanceRepository : CrudRepository<EngineInstance, String> {
    fun findByStatus(status: EngineStatus): List<EngineInstance>
    fun updateLastHeartbeat(id: String, lastHeartbeat: Instant): Int
}
