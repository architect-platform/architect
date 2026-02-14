package io.github.architectplatform.engine.core.tasks.interfaces.dto

import io.github.architectplatform.engine.domain.events.ExecutionId
import io.micronaut.serde.annotation.Serdeable

/**
 * DTO representing the status of a task execution.
 *
 * @property executionId Unique identifier of the execution
 * @property projectName Name of the project being executed
 * @property taskId ID of the task being executed
 * @property status Current status of the execution
 * @property startTime Timestamp when execution started (milliseconds since epoch)
 * @property endTime Timestamp when execution ended (milliseconds since epoch), null if still running
 * @property totalTasks Total number of tasks in the execution
 * @property completedTasks Number of tasks completed successfully
 * @property failedTasks Number of tasks that failed
 * @property skippedTasks Number of tasks that were skipped
 */
@Serdeable
data class ExecutionStatusDTO(
    val executionId: ExecutionId,
    val projectName: String,
    val taskId: String,
    val status: ExecutionStatus,
    val startTime: Long,
    val endTime: Long? = null,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val failedTasks: Int = 0,
    val skippedTasks: Int = 0
) {
    /**
     * Calculate the duration of the execution in milliseconds.
     * Returns null if execution hasn't ended yet.
     */
    fun duration(): Long? = endTime?.let { it - startTime }
    
    /**
     * Check if execution is still running.
     */
    fun isRunning(): Boolean = status == ExecutionStatus.RUNNING
    
    /**
     * Check if execution has completed (successfully or with errors).
     */
    fun isComplete(): Boolean = status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED
}

@Serdeable
enum class ExecutionStatus {
    RUNNING,
    COMPLETED,
    FAILED
}
