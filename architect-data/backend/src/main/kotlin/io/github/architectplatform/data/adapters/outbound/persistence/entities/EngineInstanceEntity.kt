package io.github.architectplatform.data.adapters.outbound.persistence.entities

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for engine instance persistence.
 * Mapped to database table.
 */
@MappedEntity("engine_instances")
data class EngineInstanceEntity(
    @field:Id
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?,
    val status: String,
    val createdAt: Instant,
    val lastHeartbeat: Instant
) {
    companion object {
        fun fromDomain(engine: EngineInstance): EngineInstanceEntity {
            return EngineInstanceEntity(
                id = engine.id,
                hostname = engine.hostname,
                port = engine.port,
                version = engine.version,
                status = engine.status.name,
                createdAt = engine.createdAt,
                lastHeartbeat = engine.lastHeartbeat
            )
        }
    }
    
    fun toDomain(): EngineInstance {
        return EngineInstance(
            id = id,
            hostname = hostname,
            port = port,
            version = version,
            status = EngineStatus.valueOf(status),
            createdAt = createdAt,
            lastHeartbeat = lastHeartbeat
        )
    }
}
