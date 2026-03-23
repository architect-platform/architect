package io.github.architectplatform.engine.core.execution

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskPermission

internal data class TaskPermissionContext(
  val taskId: String,
  val permissions: Set<TaskPermission>,
)

internal object TaskPermissionScope {
  private val current = ThreadLocal<TaskPermissionContext?>()

  fun current(): TaskPermissionContext? = current.get()

  fun <T> withTask(task: Task, block: () -> T): T =
    withPermissions(task.id, task.requiredPermissions(), block)

  fun <T> withPermissions(taskId: String, permissions: Set<TaskPermission>, block: () -> T): T {
    val previous = current.get()
    current.set(TaskPermissionContext(taskId, permissions))
    return try {
      block()
    } finally {
      if (previous == null) {
        current.remove()
      } else {
        current.set(previous)
      }
    }
  }
}