package io.github.architectplatform.engine.core.tasks.domain.events

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.plugin.domain.events.ArchitectEventDTO
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEventType
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.github.architectplatform.engine.domain.events.ExecutionTaskEvent
import io.micronaut.serde.annotation.SerdeImport
import io.micronaut.serde.annotation.Serdeable

@SerdeImport(TaskResult::class)
object TaskEvents {

  @Serdeable
  data class TaskEventDTO(
      override val project: String,
      override val executionId: ExecutionId,
      override val taskId: String,
      override val success: Boolean = true,
      override val executionEventType: ExecutionEventType,
      override val message: String? = null,
      override val errorDetails: String? = null,
      override val parentProject: String? = null,
  ) : ExecutionTaskEvent

  fun taskCompletedEvent(
      project: String,
      executionId: ExecutionId,
      taskId: String,
      success: Boolean = true,
      message: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<TaskEventDTO> {
    return ArchitectEventDTO(
        id = "task.completed",
        event =
            TaskEventDTO(
                project = project,
                executionId = executionId,
                taskId = taskId,
                success = success,
                executionEventType = ExecutionEventType.TASK_COMPLETED,
                message = message,
                parentProject = subProject))
  }

  fun taskFailedEvent(
      project: String,
      executionId: ExecutionId,
      taskId: String,
      success: Boolean = false,
      message: String? = null,
      errorDetails: String? = null,
      parentProject: String? = null,
  ): ArchitectEvent<TaskEventDTO> {
    return ArchitectEventDTO(
        id = "task.failed",
        event =
            TaskEventDTO(
                project = project,
                executionId = executionId,
                taskId = taskId,
                success = success,
                executionEventType = ExecutionEventType.FAILED,
                message = message,
                errorDetails = errorDetails,
                parentProject = parentProject))
  }

  fun taskSkippedEvent(
      project: String,
      executionId: ExecutionId,
      taskId: String,
      success: Boolean = true,
      message: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<TaskEventDTO> {
    return ArchitectEventDTO(
        id = "task.skipped",
        event =
            TaskEventDTO(
                project = project,
                executionId = executionId,
                taskId = taskId,
                success = success,
                executionEventType = ExecutionEventType.SKIPPED,
                message = message,
                parentProject = subProject))
  }

  fun taskStartedEvent(
      project: String,
      executionId: ExecutionId,
      taskId: String,
      success: Boolean = true,
      message: String? = null,
      subProject: String? = null,
  ): ArchitectEvent<TaskEventDTO> {
    return ArchitectEventDTO(
        id = "task.started",
        event =
            TaskEventDTO(
                project = project,
                executionId = executionId,
                taskId = taskId,
                success = success,
                executionEventType = ExecutionEventType.STARTED,
                message = message,
                parentProject = subProject))
  }

  fun taskOutputEvent(
      project: String,
      executionId: ExecutionId,
      taskId: String,
      output: String,
      subProject: String? = null,
  ): ArchitectEvent<TaskEventDTO> {
    return ArchitectEventDTO(
        id = "task.output",
        event =
            TaskEventDTO(
                project = project,
                executionId = executionId,
                taskId = taskId,
                success = true,
                executionEventType = ExecutionEventType.OUTPUT,
                message = output,
                parentProject = subProject))
  }
}
