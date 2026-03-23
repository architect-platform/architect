package io.github.architectplatform.engine.core.execution

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskPermission
import java.nio.file.Path

internal data class TaskPermissionContext(
  val taskId: String,
  val permissions: Set<TaskPermission>,
  val projectDir: Path? = null,
)

internal object TaskPermissionScope {
  private val current = ThreadLocal<TaskPermissionContext?>()

  fun current(): TaskPermissionContext? = current.get()

  fun <T> withTask(task: Task, projectDir: Path? = null, block: () -> T): T =
    withPermissions(task.id, task.requiredPermissions(), projectDir, block)

  fun <T> withPermissions(
    taskId: String,
    permissions: Set<TaskPermission>,
    projectDir: Path? = null,
    block: () -> T,
  ): T {
    val previous = current.get()
    current.set(TaskPermissionContext(taskId, permissions, projectDir))
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