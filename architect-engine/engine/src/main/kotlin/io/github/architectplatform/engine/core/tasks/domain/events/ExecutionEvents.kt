package io.github.architectplatform.engine.core.tasks.domain.events

import io.github.architectplatform.engine.core.plugin.domain.events.ArchitectEventDTO
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionEventType
import io.micronaut.serde.annotation.Serdeable

object ExecutionEvents {

  @Serdeable
  data class ExecutionEventDTO(
      override val project: String,
      override val executionId: String,
      override val success: Boolean,
      override val executionEventType: ExecutionEventType,
      override val message: String? = null,
      override val errorDetails: String? = null,
      override val subProject: String? = null,
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
                subProject = subProject,
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
                subProject = subProject,
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
                subProject = subProject,
            ))
  }
}
