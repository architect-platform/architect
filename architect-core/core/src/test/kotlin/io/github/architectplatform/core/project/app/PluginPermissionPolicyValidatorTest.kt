package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.tasks.infrastructure.InMemoryTaskRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginPermissionPolicyValidatorTest {

  @Test
  fun `validate emits warning when task uses undeclared permission in non-strict mode`() {
    val plugin = testPlugin("example-plugin", declaredPermissions = listOf(TaskPermission.FILE_SYSTEM_READ.wireName))
    val registry = InMemoryTaskRegistry().apply {
      add(testTask("example-plugin-run", setOf(TaskPermission.FILE_SYSTEM_READ, TaskPermission.PROCESS_EXEC)))
    }

    val result = PluginPermissionPolicyValidator.validate(
      plugins = listOf(plugin),
      taskRegistry = registry,
      tasksByPluginId = mapOf("example-plugin" to setOf("example-plugin-run")),
      strictMode = false,
    )

    assertTrue(result.errors.isEmpty())
    assertEquals(1, result.warnings.size)
    assertTrue(result.warnings.first().contains("undeclared permissions"))
  }

  @Test
  fun `validate emits error when task uses undeclared permission in strict mode`() {
    val plugin = testPlugin("example-plugin", declaredPermissions = listOf(TaskPermission.FILE_SYSTEM_READ.wireName))
    val registry = InMemoryTaskRegistry().apply {
      add(testTask("example-plugin-run", setOf(TaskPermission.FILE_SYSTEM_READ, TaskPermission.PROCESS_EXEC)))
    }

    val result = PluginPermissionPolicyValidator.validate(
      plugins = listOf(plugin),
      taskRegistry = registry,
      tasksByPluginId = mapOf("example-plugin" to setOf("example-plugin-run")),
      strictMode = true,
    )

    assertTrue(result.warnings.isEmpty())
    assertEquals(1, result.errors.size)
    assertTrue(result.errors.first().contains("undeclared permissions"))
  }

  @Test
  fun `validate passes when all task permissions are declared`() {
    val plugin =
      testPlugin(
        "example-plugin",
        declaredPermissions = listOf(
          TaskPermission.FILE_SYSTEM_READ.wireName,
          TaskPermission.PROCESS_EXEC.wireName,
        ),
      )
    val registry = InMemoryTaskRegistry().apply {
      add(testTask("example-plugin-run", setOf(TaskPermission.FILE_SYSTEM_READ, TaskPermission.PROCESS_EXEC)))
    }

    val result = PluginPermissionPolicyValidator.validate(
      plugins = listOf(plugin),
      taskRegistry = registry,
      tasksByPluginId = mapOf("example-plugin" to setOf("example-plugin-run")),
      strictMode = true,
    )

    assertTrue(result.warnings.isEmpty())
    assertTrue(result.errors.isEmpty())
  }

  private fun testPlugin(id: String, declaredPermissions: List<String>): ArchitectPlugin<Any> =
    object : ArchitectPlugin<Any> {
      override val id: String = id
      override val contextKey: String = id
      override val ctxClass: Class<Any> = Any::class.java
      override var context: Any = Unit
      override fun register(registry: TaskRegistry) = Unit
      override fun configSchema(): Map<String, Any> = mapOf(
        "type" to "object",
        "x-permissions" to declaredPermissions,
      )
    }

  private fun testTask(id: String, permissions: Set<TaskPermission>): Task =
    object : Task {
      override val id: String = id
      override fun requiredPermissions(): Set<TaskPermission> = permissions
      override fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>,
      ): TaskResult = TaskResult.success()
    }
}
