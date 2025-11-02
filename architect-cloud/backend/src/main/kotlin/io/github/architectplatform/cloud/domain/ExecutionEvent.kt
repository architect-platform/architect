package io.github.architectplatform.cloud.domain

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

/**
 * Represents an event that occurred during an execution.
 */
@Serdeable
@MappedEntity("execution_events")
data class ExecutionEvent(
    @field:Id
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String? = null,
    val message: String? = null,
    val output: String? = null,
    val success: Boolean = true,
    @DateCreated
    val timestamp: Instant? = null
)
