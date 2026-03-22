package io.github.architectplatform.plugins.docker

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DockerPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = DockerPlugin()
    assertEquals("docker-plugin", plugin.id)
    assertEquals("docker", plugin.contextKey)
    assertEquals(DockerContext::class.java, plugin.ctxClass)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = DockerPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("docker-build" in ids)
    assertTrue("docker-push" in ids)
    assertTrue("docker-run" in ids)
    assertTrue("docker-compose-up" in ids)
    assertTrue("docker-compose-down" in ids)
    assertTrue("docker-compose-logs" in ids)
    assertEquals(6, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = DockerPlugin()
    val ctx = DockerContext(image = "myapp:v1")
    plugin.init(ctx)
    assertEquals("myapp:v1", plugin.context.image)
  }

  /** Minimal TaskRegistry stub for testing registration. */
  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
