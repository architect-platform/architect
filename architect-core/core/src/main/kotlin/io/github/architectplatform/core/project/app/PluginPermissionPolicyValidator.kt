package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Validates plugin task permissions against plugin-declared capabilities.
 *
 * Plugins declare allowed task permissions in `configSchema()` using:
 * {
 *   "x-permissions": ["file-system:read", ...]
 * }
 */
object PluginPermissionPolicyValidator {

  fun validate(
    plugins: List<ArchitectPlugin<*>>,
    taskRegistry: TaskRegistry,
    tasksByPluginId: Map<String, Set<String>>,
    strictMode: Boolean,
  ): ValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    val taskById = taskRegistry.all().associateBy(Task::id)
    plugins.forEach { plugin ->
      val declared = declaredPermissions(plugin)
      if (declared == null) return@forEach

      tasksByPluginId[plugin.id].orEmpty().forEach taskLoop@ { taskId ->
          val task = taskById[taskId] ?: return@taskLoop
          val actual = task.requiredPermissions()
          val undeclared = actual - declared
          if (undeclared.isEmpty()) return@taskLoop

          val message =
            "Plugin '${plugin.id}' task '${task.id}' uses undeclared permissions: " +
              undeclared.joinToString(", ") { it.wireName } +
              ". Declare them via configSchema x-permissions."

          if (strictMode) errors += message else warnings += message
        }
    }

    return ValidationResult(errors = errors, warnings = warnings)
  }

  private fun declaredPermissions(plugin: ArchitectPlugin<*>): Set<TaskPermission>? {
    val schema = plugin.configSchema() ?: return null
    val rawPermissions = schema["x-permissions"] as? Collection<*> ?: return null
    val names = rawPermissions.mapNotNull { it as? String }
    if (names.isEmpty()) return emptySet()
    return TaskPermission.fromWireNames(names)
  }

  data class ValidationResult(
    val errors: List<String>,
    val warnings: List<String>,
  )
}
