package io.github.architectplatform.plugins.python

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
