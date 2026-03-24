package io.github.architectplatform.plugins.python

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class PythonPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = PythonPlugin()
    assertEquals("python-plugin", plugin.id)
    assertEquals("python", plugin.contextKey)
  }

  @Test
  fun `default context uses uv`() {
    val ctx = PythonContext()
    assertEquals("uv", ctx.tool)
    assertEquals("pytest", ctx.testRunner)
    assertEquals("ruff", ctx.linter)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = PythonPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("py-install" in ids)
    assertTrue("py-lint" in ids)
    assertTrue("py-test" in ids)
    assertTrue("py-build" in ids)
    assertTrue("py-publish" in ids)
    assertEquals(5, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = PythonPlugin()
    plugin.init(PythonContext(tool = "poetry", pythonVersion = "3.12"))
    assertEquals("poetry", plugin.context.tool)
    assertEquals("3.12", plugin.context.pythonVersion)
  }

  @Test
  fun `py-install uses uv sync by default`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(PythonContext(tool = "uv"), "py-install")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("uv sync", executor.command)
  }

  @Test
  fun `py-install uses poetry when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(PythonContext(tool = "poetry"), "py-install")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("poetry install", executor.command)
  }

  @Test
  fun `py-install falls back to pip`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(PythonContext(tool = "pip"), "py-install")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("pip install -r requirements.txt", executor.command)
  }

  @Test
  fun `py-test uses pytest by default`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(PythonContext(), "py-test")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("pytest", executor.command)
  }

  @Test
  fun `py-lint uses ruff by default`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(PythonContext(linter = "ruff"), "py-lint")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("ruff check '.'", executor.command)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(PythonContext(enabled = false), "py-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  private fun registerAndGet(ctx: PythonContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = PythonPlugin()
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

  private class TestEnvironment(private val commandExecutor: CommandExecutor) : Environment {
    override fun <T> service(type: Class<T>): T {
      if (type == CommandExecutor::class.java) {
        @Suppress("UNCHECKED_CAST")
        return commandExecutor as T
      }
      throw IllegalArgumentException("Unsupported service: \${type.name}")
    }
    override fun publish(event: Any) {}
  }
}
