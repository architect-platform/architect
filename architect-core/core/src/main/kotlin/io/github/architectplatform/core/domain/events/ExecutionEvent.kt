package io.github.architectplatform.core.domain.events
//
// Execution
//

typealias ExecutionId = String

interface ExecutionEvent {
  val project: String
  val executionId: ExecutionId
  val executionEventType: ExecutionEventType
  val success: Boolean
  val message: String?
  val errorDetails: String?
  val parentProject: String?
}

enum class ExecutionEventType {
  STARTED,
  UPDATED,
  COMPLETED,
  FAILED,
  SKIPPED,
  OUTPUT,
  TASK_COMPLETED
}
