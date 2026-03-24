package io.github.architectplatform.plugins.javascript

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class JavaScriptPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = JavaScriptPlugin()
    assertEquals("javascript-plugin", plugin.id)
    assertEquals("javascript", plugin.contextKey)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = JavaScriptPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("javascript-install" in ids)
    assertTrue("javascript-build" in ids)
    assertTrue("javascript-test" in ids)
    assertTrue("javascript-lint" in ids)
    assertTrue("javascript-dev" in ids)
    assertEquals(5, ids.size)
  }

  @Test
  fun `javascript-install uses npm by default`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-install")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("npm install", executor.command)
  }

  @Test
  fun `javascript-build uses yarn when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "yarn"), "javascript-build")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("yarn build", executor.command)
  }

  @Test
  fun `javascript-test uses pnpm when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "pnpm"), "javascript-test")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("pnpm test", executor.command)
  }

  @Test
  fun `javascript-lint uses npm run lint`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-lint")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("npm run lint", executor.command)
  }

  @Test
  fun `javascript-dev uses pnpm dev`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "pnpm"), "javascript-dev")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("pnpm dev", executor.command)
  }

  @Test
  fun `task passes extra args`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-test")

    task.execute(TestEnvironment(executor), projectContext(), listOf("--coverage"))

    assertEquals("npm test '--coverage'", executor.command)
  }

  @Test
  fun `task returns failure on executor exception`() {
    val task = registerAndGet(JavaScriptContext(), "javascript-build")

    val result = task.execute(TestEnvironment(FailingCommandExecutor()), projectContext(), emptyList())

    assertFalse(result.success)
  }

  @Test
  fun `task rejects working directory traversal`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(workingDirectory = "../outside"), "javascript-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertFalse(result.success)
    assertNull(executor.command)
    assertTrue(result.message!!.contains("invalid working directory"))
  }

  private fun registerAndGet(ctx: JavaScriptContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = JavaScriptPlugin()
    plugin.init(ctx)
    val registry = TestTaskRegistry()
    plugin.register(registry)
    return registry.get(taskId)!!
  }

  private fun projectContext() = ProjectContext(Path.of("/repo"), emptyMap())

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }

  private class RecordingCommandExecutor : CommandExecutor {
    var command: String? = null
    var workingDir: String? = null
    override fun execute(command: String, workingDir: String?) {
      this.command = command
      this.workingDir = workingDir
    }
  }

  private class FailingCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {
      throw RuntimeException("Command failed")
    }
  }

  private class TestEnvironment(private val commandExecutor: CommandExecutor) : Environment {
    override fun <T> service(type: Class<T>): T {
      if (type == CommandExecutor::class.java) {
        @Suppress("UNCHECKED_CAST")
        return commandExecutor as T
      }
      throw IllegalArgumentException("Unsupported service: ${type.name}")
    }
    override fun publish(event: Any) {}
  }
}
