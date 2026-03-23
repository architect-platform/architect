package io.github.architectplatform.plugins.terraform

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

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

  @Test
  fun `tf-init executes with backend config`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(TerraformContext(backend = "config.hcl"), "tf-init")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("terraform init -backend-config=config.hcl", executor.command)
  }

  @Test
  fun `tf-plan includes var flags`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(TerraformContext(vars = mapOf("region" to "us-east-1"), varFile = "prod.tfvars"), "tf-plan")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(executor.command!!.contains("-var 'region=us-east-1'"))
    assertTrue(executor.command!!.contains("-var-file=prod.tfvars"))
  }

  @Test
  fun `tf-apply includes auto-approve when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(TerraformContext(autoApprove = true), "tf-apply")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(executor.command!!.contains("-auto-approve"))
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(TerraformContext(enabled = false), "tf-init")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  private fun registerAndGet(ctx: TerraformContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = TerraformPlugin()
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
