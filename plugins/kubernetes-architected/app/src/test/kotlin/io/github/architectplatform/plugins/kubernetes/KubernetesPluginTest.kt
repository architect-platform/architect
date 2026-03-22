package io.github.architectplatform.plugins.kubernetes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KubernetesPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = KubernetesPlugin()
    assertEquals("kubernetes-plugin", plugin.id)
    assertEquals("kubernetes", plugin.contextKey)
  }

  @Test
  fun `default context has sensible defaults`() {
    val ctx = KubernetesContext()
    assertEquals("default", ctx.namespace)
    assertEquals("", ctx.context)
    assertEquals("k8s/", ctx.manifests)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = KubernetesPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("k8s-apply" in ids)
    assertTrue("k8s-rollout" in ids)
    assertTrue("k8s-status" in ids)
    assertTrue("k8s-port-forward" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = KubernetesPlugin()
    val ctx = KubernetesContext(namespace = "prod", context = "aws-prod")
    plugin.init(ctx)
    assertEquals("prod", plugin.context.namespace)
    assertEquals("aws-prod", plugin.context.context)
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
