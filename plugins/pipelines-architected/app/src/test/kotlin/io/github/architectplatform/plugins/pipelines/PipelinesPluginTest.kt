package io.github.architectplatform.plugins.pipelines

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class PipelinesPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = PipelinesPlugin()
    assertEquals("pipelines-plugin", plugin.id)
    assertEquals("pipelines", plugin.contextKey)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = PipelinesPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("pipelines-init" in ids)
    assertTrue("pipelines-execute" in ids)
    assertTrue("pipelines-list" in ids)
    assertEquals(3, ids.size)
  }

  @Test
  fun `pipelines-list returns no workflows when none configured`() {
    val task = registerAndGet(PipelinesContext(workflows = emptyList()), "pipelines-list")

    val result = task.execute(TestEnvironment(), projectContext(), emptyList())

    assertTrue(result.success)
    assertTrue(result.message!!.contains("No workflows"))
  }

  @Test
  fun `pipelines-list returns configured workflows`() {
    val task = registerAndGet(PipelinesContext(workflows = listOf(
      WorkflowDefinition(name = "ci", description = "CI pipeline"),
      WorkflowDefinition(name = "release", description = "Release pipeline")
    )), "pipelines-list")

    val result = task.execute(TestEnvironment(), projectContext(), emptyList())

    assertTrue(result.success)
    assertTrue(result.message!!.contains("ci"))
    assertTrue(result.message!!.contains("release"))
  }

  @Test
  fun `pipelines-list shows disabled message when plugin disabled`() {
    val task = registerAndGet(PipelinesContext(enabled = false), "pipelines-list")

    val result = task.execute(TestEnvironment(), projectContext(), emptyList())

    assertTrue(result.success)
    assertTrue(result.message!!.contains("disabled"))
  }

  @Test
  fun `pipelines-execute fails when no workflow name provided`() {
    val task = registerAndGet(PipelinesContext(), "pipelines-execute")

    val result = task.execute(TestEnvironment(), projectContext(), emptyList())

    assertFalse(result.success)
    assertTrue(result.message!!.contains("No workflow name"))
  }

  @Test
  fun `pipelines-execute fails for unknown workflow`() {
    val task = registerAndGet(PipelinesContext(workflows = listOf(
      WorkflowDefinition(name = "ci")
    )), "pipelines-execute")

    val result = task.execute(TestEnvironment(), projectContext(), listOf("unknown"))

    assertFalse(result.success)
    assertTrue(result.message!!.contains("not found"))
  }

  @Test
  fun `pipelines-execute skips when disabled`() {
    val task = registerAndGet(PipelinesContext(enabled = false), "pipelines-execute")

    val result = task.execute(TestEnvironment(), projectContext(), listOf("ci"))

    assertTrue(result.success)
    assertTrue(result.message!!.contains("skipped"))
  }

  private fun registerAndGet(ctx: PipelinesContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = PipelinesPlugin()
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

  private class TestEnvironment : Environment {
    override fun <T> service(type: Class<T>): T {
      throw IllegalArgumentException("Unsupported service: ${type.name}")
    }
    override fun publish(event: Any) {}
  }
}
