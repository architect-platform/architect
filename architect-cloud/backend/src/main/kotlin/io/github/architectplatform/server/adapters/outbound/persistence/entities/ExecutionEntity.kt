package io.github.architectplatform.server.adapters.outbound.persistence.entities

import io.github.architectplatform.server.application.domain.Execution
import io.github.architectplatform.server.application.domain.ExecutionStatus
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for execution persistence.
 * Maps domain model to database table.
 */
@MappedEntity("executions")
data class ExecutionEntity(
    @field:Id
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: String,
    val message: String?,
    val errorDetails: String?,
    @DateCreated
    val startedAt: Instant? = null,
    val completedAt: Instant?
) {
    fun toDomain(): Execution {
        return Execution(
            id = id,
            projectId = projectId,
            engineId = engineId,
            taskId = taskId,
            status = ExecutionStatus.valueOf(status),
            message = message,
            errorDetails = errorDetails,
            startedAt = startedAt ?: Instant.now(),
            completedAt = completedAt
        )
    }
    
    companion object {
        fun fromDomain(domain: Execution): ExecutionEntity {
            return ExecutionEntity(
                id = domain.id,
                projectId = domain.projectId,
                engineId = domain.engineId,
                taskId = domain.taskId,
                status = domain.status.name,
                message = domain.message,
                errorDetails = domain.errorDetails,
                startedAt = domain.startedAt,
                completedAt = domain.completedAt
            )
        }
    }
}
