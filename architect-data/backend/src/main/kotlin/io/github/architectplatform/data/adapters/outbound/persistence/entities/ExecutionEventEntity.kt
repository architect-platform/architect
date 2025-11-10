package io.github.architectplatform.data.adapters.outbound.persistence.entities

import io.github.architectplatform.data.application.domain.ExecutionEvent
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for execution event persistence.
 * Mapped to database table.
 */
@MappedEntity("execution_events")
data class ExecutionEventEntity(
    @field:Id
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String?,
    val message: String?,
    val output: String?,
    val success: Boolean,
    val timestamp: Instant
) {
    companion object {
        fun fromDomain(event: ExecutionEvent): ExecutionEventEntity {
            return ExecutionEventEntity(
                id = event.id,
                executionId = event.executionId,
                eventType = event.eventType,
                taskId = event.taskId,
                message = event.message,
                output = event.output,
                success = event.success,
                timestamp = event.timestamp
            )
        }
    }
    
    fun toDomain(): ExecutionEvent {
        return ExecutionEvent(
            id = id,
            executionId = executionId,
            eventType = eventType,
            taskId = taskId,
            message = message,
            output = output,
            success = success,
            timestamp = timestamp
        )
    }
}
