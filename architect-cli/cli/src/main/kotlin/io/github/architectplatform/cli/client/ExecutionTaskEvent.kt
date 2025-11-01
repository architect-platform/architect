package io.github.architectplatform.cli.client

import io.github.architectplatform.cli.dto.TaskResultDTO
import io.micronaut.serde.annotation.Serdeable

/**
 * Type alias for execution identifiers.
 */
typealias ExecutionId = String

/**
 * Type alias for task identifiers.
 */
typealias TaskId = String

/**
 * Event indicating that task execution has completed.
 *
 * @property executionId Unique identifier for the execution
 * @property result The result of the execution
 * @property message Status message
 * @property success Whether the execution was successful
 */
@Serdeable
class ExecutionCompletedEvent(
    executionId: ExecutionId,
    val result: TaskResultDTO,
    message: String = "Execution completed",
    success: Boolean = true,
) : ExecutionEvent(executionId, null, success, message)

/**
 * Base class for execution events.
 *
 * @property executionId Unique identifier for the execution
 * @property taskId Optional task identifier if event is task-specific
 * @property success Whether the operation was successful
 * @property message Descriptive message about the event
 */
@Serdeable
open class ExecutionEvent(
    val executionId: ExecutionId,
    val taskId: TaskId? = null,
    val success: Boolean = true,
    val message: String = "Execution event"
)
