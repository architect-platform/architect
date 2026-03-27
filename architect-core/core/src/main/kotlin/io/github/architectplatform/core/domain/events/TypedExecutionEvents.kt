package io.github.architectplatform.core.domain.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Typed event hierarchy for execution streaming and replay.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes(
  JsonSubTypes.Type(value = TaskStartedEvent::class, name = "TASK_STARTED"),
  JsonSubTypes.Type(value = TaskCompletedEvent::class, name = "TASK_COMPLETED"),
  JsonSubTypes.Type(value = TaskFailedEvent::class, name = "TASK_FAILED"),
  JsonSubTypes.Type(value = TaskOutputEvent::class, name = "TASK_OUTPUT"),
  JsonSubTypes.Type(value = TaskRetryingEvent::class, name = "TASK_RETRYING"),
  JsonSubTypes.Type(value = TaskSkippedEvent::class, name = "TASK_SKIPPED"),
  JsonSubTypes.Type(value = ExecutionStartedEvent::class, name = "EXECUTION_STARTED"),
  JsonSubTypes.Type(value = ExecutionCompletedEvent::class, name = "EXECUTION_COMPLETED"),
  JsonSubTypes.Type(value = ExecutionFailedEvent::class, name = "EXECUTION_FAILED"),
  JsonSubTypes.Type(value = ExecutionCancelledEvent::class, name = "EXECUTION_CANCELLED"),
)
sealed interface TypedExecutionEvent {
  val project: String
  val executionId: ExecutionId
  val success: Boolean
  val message: String?
  val errorDetails: String?
  val parentProject: String?
  val eventType: String
}

sealed interface TypedTaskEvent : TypedExecutionEvent {
  val taskId: TaskId
}

data class TaskStartedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val taskId: TaskId,
  override val success: Boolean = true,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedTaskEvent {
  override val eventType: String = "TASK_STARTED"
}

data class TaskCompletedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val taskId: TaskId,
  override val success: Boolean = true,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedTaskEvent {
  override val eventType: String = "TASK_COMPLETED"
}

data class TaskFailedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val taskId: TaskId,
  override val success: Boolean = false,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedTaskEvent {
  override val eventType: String = "TASK_FAILED"
}

data class TaskOutputEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val taskId: TaskId,
  override val success: Boolean = true,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedTaskEvent {
  override val eventType: String = "TASK_OUTPUT"
}

data class TaskRetryingEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val taskId: TaskId,
  override val success: Boolean = false,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedTaskEvent {
  override val eventType: String = "TASK_RETRYING"
}

data class TaskSkippedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val taskId: TaskId,
  override val success: Boolean = true,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedTaskEvent {
  override val eventType: String = "TASK_SKIPPED"
}

data class ExecutionStartedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val success: Boolean = true,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedExecutionEvent {
  override val eventType: String = "EXECUTION_STARTED"
}

data class ExecutionCompletedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val success: Boolean = true,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedExecutionEvent {
  override val eventType: String = "EXECUTION_COMPLETED"
}

data class ExecutionFailedEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val success: Boolean = false,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedExecutionEvent {
  override val eventType: String = "EXECUTION_FAILED"
}

data class ExecutionCancelledEvent(
  override val project: String,
  override val executionId: ExecutionId,
  override val success: Boolean = false,
  override val message: String? = null,
  override val errorDetails: String? = null,
  override val parentProject: String? = null,
) : TypedExecutionEvent {
  override val eventType: String = "EXECUTION_CANCELLED"
}

data class PluginLoadedEvent(
  val pluginId: PluginId,
) {
  val eventType: String = "PLUGIN_LOADED"
}

data class ProjectRegisteredEvent(
  val projectName: String,
  val projectPath: String,
) {
  val eventType: String = "PROJECT_REGISTERED"
}
