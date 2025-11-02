package io.github.architectplatform.cloud.domain

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

/**
 * Represents a task execution.
 */
@Serdeable
@MappedEntity("executions")
data class Execution(
    @field:Id
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: ExecutionStatus,
    val message: String? = null,
    val errorDetails: String? = null,
    @DateCreated
    val startedAt: Instant? = null,
    val completedAt: Instant? = null
)

@Serdeable
enum class ExecutionStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}
