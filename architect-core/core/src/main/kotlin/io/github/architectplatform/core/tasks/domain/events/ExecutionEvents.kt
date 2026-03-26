package io.github.architectplatform.core.tasks.domain.events

import io.github.architectplatform.core.plugin.domain.events.ArchitectEventDTO
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.ExecutionEventType

object ExecutionEvents {

  data class ExecutionEventDTO(
	  override val project: String,
	  override val executionId: String,
	  override val success: Boolean,
	  override val executionEventType: ExecutionEventType,
	  override val message: String? = null,
	  override val errorDetails: String? = null,
	  override val parentProject: String? = null,
  ) : ExecutionEvent

  fun executionStartedEvent(
      project: String,
      executionId: String,
      success: Boolean = true,
      message: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<ExecutionEvent> {
    return ArchitectEventDTO(
        id = "execution.started",
        event =
            ExecutionEventDTO(
                project = project,
                executionId = executionId,
                success = success,
                executionEventType = ExecutionEventType.STARTED,
                message = message,
                parentProject = subProject,
            ))
  }

  fun executionCompletedEvent(
      project: String,
      executionId: String,
      success: Boolean = true,
      message: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<ExecutionEvent> {
    return ArchitectEventDTO(
        id = "execution.completed",
        event =
            ExecutionEventDTO(
                project = project,
                executionId = executionId,
                success = success,
                executionEventType = ExecutionEventType.COMPLETED,
                message = message,
                parentProject = subProject,
            ))
  }

  fun executionFailedEvent(
      project: String,
      executionId: String,
      success: Boolean = false,
      message: String? = null,
      errorDetails: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<ExecutionEvent> {
    return ArchitectEventDTO(
        id = "execution.failed",
        event =
            ExecutionEventDTO(
                project = project,
                executionId = executionId,
                success = success,
                executionEventType = ExecutionEventType.FAILED,
                message = message,
                errorDetails = errorDetails,
                parentProject = subProject,
            ))
  }

  fun executionCancelledEvent(
      project: String,
      executionId: String,
      message: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<ExecutionEvent> {
    return ArchitectEventDTO(
        id = "execution.cancelled",
        event =
            ExecutionEventDTO(
                project = project,
                executionId = executionId,
                success = false,
                executionEventType = ExecutionEventType.CANCELLED,
                message = message ?: "Execution cancelled",
                parentProject = subProject,
            ))
  }
}
