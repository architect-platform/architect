package io.github.architectplatform.cloud.domain

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

/**
 * Represents a project registered in the cloud.
 */
@Serdeable
@MappedEntity("projects")
data class Project(
    @field:Id
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String? = null,
    @DateCreated
    val createdAt: Instant? = null
)
