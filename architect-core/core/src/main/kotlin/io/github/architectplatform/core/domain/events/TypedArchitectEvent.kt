package io.github.architectplatform.core.domain.events

data class TypedArchitectEvent(
  override val id: ArchitectEventId,
  val type: String,
  override val event: TypedExecutionEvent? = null,
) : AbstractArchitectEvent<TypedExecutionEvent>(id, event)

fun ArchitectEvent<ExecutionEvent>.toTypedArchitectEvent(): TypedArchitectEvent {
  val e = event ?: return TypedArchitectEvent(id = id, type = "UNKNOWN", event = null)
  val typed: TypedExecutionEvent =
    when (id) {
      "task.started" -> TaskStartedEvent(
        project = e.project,
        executionId = e.executionId,
        taskId = (e as? ExecutionTaskEvent)?.taskId ?: "unknown",
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "task.completed" -> TaskCompletedEvent(
        project = e.project,
        executionId = e.executionId,
        taskId = (e as? ExecutionTaskEvent)?.taskId ?: "unknown",
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "task.failed" -> TaskFailedEvent(
        project = e.project,
        executionId = e.executionId,
        taskId = (e as? ExecutionTaskEvent)?.taskId ?: "unknown",
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "task.output" -> TaskOutputEvent(
        project = e.project,
        executionId = e.executionId,
        taskId = (e as? ExecutionTaskEvent)?.taskId ?: "unknown",
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "task.retrying" -> TaskRetryingEvent(
        project = e.project,
        executionId = e.executionId,
        taskId = (e as? ExecutionTaskEvent)?.taskId ?: "unknown",
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "task.skipped" -> TaskSkippedEvent(
        project = e.project,
        executionId = e.executionId,
        taskId = (e as? ExecutionTaskEvent)?.taskId ?: "unknown",
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "execution.started" -> ExecutionStartedEvent(
        project = e.project,
        executionId = e.executionId,
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "execution.completed" -> ExecutionCompletedEvent(
        project = e.project,
        executionId = e.executionId,
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "execution.failed" -> ExecutionFailedEvent(
        project = e.project,
        executionId = e.executionId,
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      "execution.cancelled" -> ExecutionCancelledEvent(
        project = e.project,
        executionId = e.executionId,
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
      else -> ExecutionStartedEvent(
        project = e.project,
        executionId = e.executionId,
        success = e.success,
        message = e.message,
        errorDetails = e.errorDetails,
        parentProject = e.parentProject,
      )
    }

  return TypedArchitectEvent(
    id = id,
    type = typed.eventType,
    event = typed,
  )
}
