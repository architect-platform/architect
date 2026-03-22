package io.github.architectplatform.plugins.rust

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RustPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = RustPlugin()
    assertEquals("rust-plugin", plugin.id)
    assertEquals("rust", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = RustContext()
    assertEquals("release", ctx.profile)
    assertTrue(ctx.features.isEmpty())
    assertEquals("", ctx.target)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = RustPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("cargo-build" in ids)
    assertTrue("cargo-test" in ids)
    assertTrue("cargo-lint" in ids)
    assertTrue("cargo-publish" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = RustPlugin()
    plugin.init(RustContext(profile = "debug", features = listOf("serde", "tokio"), target = "x86_64-unknown-linux-gnu"))
    assertEquals("debug", plugin.context.profile)
    assertEquals(listOf("serde", "tokio"), plugin.context.features)
    assertEquals("x86_64-unknown-linux-gnu", plugin.context.target)
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
