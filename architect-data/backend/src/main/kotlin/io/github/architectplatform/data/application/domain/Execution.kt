package io.github.architectplatform.data.application.domain

import java.time.Instant

/**
 * Domain model representing a workflow/task execution.
 * Pure domain object without infrastructure concerns.
 */
data class Execution(
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: ExecutionStatus,
    val message: String? = null,
    val errorDetails: String? = null,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null
) {
    fun complete(message: String? = null): Execution {
        return copy(
            status = ExecutionStatus.COMPLETED,
            message = message,
            completedAt = Instant.now()
        )
    }
    
    fun fail(errorDetails: String, message: String? = null): Execution {
        return copy(
            status = ExecutionStatus.FAILED,
            errorDetails = errorDetails,
            message = message,
            completedAt = Instant.now()
        )
    }
    
    fun updateStatus(newStatus: ExecutionStatus, message: String? = null): Execution {
        return copy(
            status = newStatus,
            message = message,
            completedAt = if (newStatus in listOf(ExecutionStatus.COMPLETED, ExecutionStatus.FAILED)) 
                Instant.now() else completedAt
        )
    }
}

enum class ExecutionStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}
