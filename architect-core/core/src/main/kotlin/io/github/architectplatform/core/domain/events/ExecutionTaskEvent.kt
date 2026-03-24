package io.github.architectplatform.core.domain.events

typealias TaskId = String

interface ExecutionTaskEvent : ExecutionEvent {
  val taskId: TaskId
}
