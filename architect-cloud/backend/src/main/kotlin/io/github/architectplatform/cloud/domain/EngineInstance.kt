package io.github.architectplatform.cloud.domain

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

/**
 * Represents an engine instance that reports to the cloud.
 */
@Serdeable
@MappedEntity("engine_instances")
data class EngineInstance(
    @field:Id
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?,
    val status: EngineStatus = EngineStatus.ACTIVE,
    @DateCreated
    val createdAt: Instant? = null,
    @DateUpdated
    val lastHeartbeat: Instant? = null
)

@Serdeable
enum class EngineStatus {
    ACTIVE,
    INACTIVE,
    OFFLINE
}
