package io.github.architectplatform.cli.client

import io.github.architectplatform.cli.dto.TaskResultDTO
import io.micronaut.serde.annotation.Serdeable

typealias ExecutionId = String

typealias TaskId = String

@Serdeable
class ExecutionCompletedEvent(
    executionId: ExecutionId,
    val result: TaskResultDTO,
    message: String = "Execution completed",
    success: Boolean = true,
) : ExecutionEvent(executionId, null, success, message)

@Serdeable
open class ExecutionEvent(
    val executionId: ExecutionId,
    val taskId: TaskId? = null,
    val success: Boolean = true,
    val message: String = "Execution event"
)
