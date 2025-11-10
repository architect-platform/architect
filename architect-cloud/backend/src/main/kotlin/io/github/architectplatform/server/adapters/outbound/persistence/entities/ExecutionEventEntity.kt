package io.github.architectplatform.server.adapters.outbound.persistence.entities

import io.github.architectplatform.server.application.domain.ExecutionEvent
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for execution event persistence.
 * Maps domain model to database table.
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
    @DateCreated
    val timestamp: Instant? = null
) {
    fun toDomain(): ExecutionEvent {
        return ExecutionEvent(
            id = id,
            executionId = executionId,
            eventType = eventType,
            taskId = taskId,
            message = message,
            output = output,
            success = success,
            timestamp = timestamp ?: Instant.now()
        )
    }
    
    companion object {
        fun fromDomain(domain: ExecutionEvent): ExecutionEventEntity {
            return ExecutionEventEntity(
                id = domain.id,
                executionId = domain.executionId,
                eventType = domain.eventType,
                taskId = domain.taskId,
                message = domain.message,
                output = domain.output,
                success = domain.success,
                timestamp = domain.timestamp
            )
        }
    }
}
