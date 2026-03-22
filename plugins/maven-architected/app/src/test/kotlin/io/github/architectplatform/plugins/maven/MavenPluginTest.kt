package io.github.architectplatform.plugins.maven

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MavenPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = MavenPlugin()
    assertEquals("maven-plugin", plugin.id)
    assertEquals("maven", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = MavenContext()
    assertTrue(ctx.profiles.isEmpty())
    assertEquals("", ctx.settings)
    assertFalse(ctx.skipTests)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = MavenPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("mvn-verify" in ids)
    assertTrue("mvn-package" in ids)
    assertTrue("mvn-deploy" in ids)
    assertEquals(3, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = MavenPlugin()
    plugin.init(MavenContext(profiles = listOf("release"), skipTests = true))
    assertEquals(listOf("release"), plugin.context.profiles)
    assertTrue(plugin.context.skipTests)
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
