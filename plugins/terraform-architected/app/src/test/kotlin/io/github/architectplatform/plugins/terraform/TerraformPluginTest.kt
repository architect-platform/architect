package io.github.architectplatform.plugins.terraform

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TerraformPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = TerraformPlugin()
    assertEquals("terraform-plugin", plugin.id)
    assertEquals("terraform", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = TerraformContext()
    assertEquals("default", ctx.workspace)
    assertEquals("", ctx.backend)
    assertTrue(ctx.vars.isEmpty())
    assertFalse(ctx.autoApprove)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = TerraformPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("tf-init" in ids)
    assertTrue("tf-plan" in ids)
    assertTrue("tf-apply" in ids)
    assertTrue("tf-destroy" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = TerraformPlugin()
    plugin.init(TerraformContext(workspace = "staging", autoApprove = true))
    assertEquals("staging", plugin.context.workspace)
    assertTrue(plugin.context.autoApprove)
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }
}
