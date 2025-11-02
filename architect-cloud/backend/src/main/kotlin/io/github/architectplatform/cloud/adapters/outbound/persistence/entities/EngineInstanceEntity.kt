package io.github.architectplatform.cloud.adapters.outbound.persistence.entities

import io.github.architectplatform.cloud.application.domain.EngineInstance
import io.github.architectplatform.cloud.application.domain.EngineStatus
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for engine instance persistence.
 * Maps domain model to database table.
 */
@MappedEntity("engine_instances")
data class EngineInstanceEntity(
    @field:Id
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?,
    val status: String,
    @DateCreated
    val createdAt: Instant? = null,
    @DateUpdated
    val lastHeartbeat: Instant? = null
) {
    fun toDomain(): EngineInstance {
        return EngineInstance(
            id = id,
            hostname = hostname,
            port = port,
            version = version,
            status = EngineStatus.valueOf(status),
            createdAt = createdAt ?: Instant.now(),
            lastHeartbeat = lastHeartbeat ?: Instant.now()
        )
    }
    
    companion object {
        fun fromDomain(domain: EngineInstance): EngineInstanceEntity {
            return EngineInstanceEntity(
                id = domain.id,
                hostname = domain.hostname,
                port = domain.port,
                version = domain.version,
                status = domain.status.name,
                createdAt = domain.createdAt,
                lastHeartbeat = domain.lastHeartbeat
            )
        }
    }
}
