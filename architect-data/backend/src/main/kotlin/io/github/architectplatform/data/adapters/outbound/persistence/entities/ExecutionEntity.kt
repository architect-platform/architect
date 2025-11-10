package io.github.architectplatform.data.adapters.outbound.persistence.entities

import io.github.architectplatform.data.application.domain.Execution
import io.github.architectplatform.data.application.domain.ExecutionStatus
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for execution persistence.
 * Mapped to database table.
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
    val startedAt: Instant,
    val completedAt: Instant?
) {
    companion object {
        fun fromDomain(execution: Execution): ExecutionEntity {
            return ExecutionEntity(
                id = execution.id,
                projectId = execution.projectId,
                engineId = execution.engineId,
                taskId = execution.taskId,
                status = execution.status.name,
                message = execution.message,
                errorDetails = execution.errorDetails,
                startedAt = execution.startedAt,
                completedAt = execution.completedAt
            )
        }
    }
    
    fun toDomain(): Execution {
        return Execution(
            id = id,
            projectId = projectId,
            engineId = engineId,
            taskId = taskId,
            status = ExecutionStatus.valueOf(status),
            message = message,
            errorDetails = errorDetails,
            startedAt = startedAt,
            completedAt = completedAt
        )
    }
}
