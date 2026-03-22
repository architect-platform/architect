package io.github.architectplatform.plugins.go

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GoPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = GoPlugin()
    assertEquals("go-plugin", plugin.id)
    assertEquals("go", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = GoContext()
    assertEquals("", ctx.module)
    assertEquals("", ctx.ldflags)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = GoPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("go-build" in ids)
    assertTrue("go-test" in ids)
    assertTrue("go-lint" in ids)
    assertTrue("go-release" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = GoPlugin()
    plugin.init(GoContext(module = "github.com/org/repo", ldflags = "-s -w"))
    assertEquals("github.com/org/repo", plugin.context.module)
    assertEquals("-s -w", plugin.context.ldflags)
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
