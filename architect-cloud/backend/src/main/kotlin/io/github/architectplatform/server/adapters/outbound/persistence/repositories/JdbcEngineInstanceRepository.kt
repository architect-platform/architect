package io.github.architectplatform.server.adapters.outbound.persistence.repositories

import io.github.architectplatform.server.adapters.outbound.persistence.entities.EngineInstanceEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.Instant

@Repository
@JdbcRepository(dialect = Dialect.H2)
interface JdbcEngineInstanceRepository : CrudRepository<EngineInstanceEntity, String> {
    fun findByStatus(status: String): List<EngineInstanceEntity>
    fun updateLastHeartbeat(id: String, lastHeartbeat: Instant): Int
}
