package io.github.architectplatform.plugins.kubernetes

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

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

  @Test
  fun `k8s-apply executes kubectl with namespace and context flags`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(KubernetesContext(namespace = "prod", context = "aws-prod"), "k8s-apply")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("kubectl apply -n prod --context aws-prod -f k8s/", executor.command)
  }

  @Test
  fun `k8s-status defaults to pods`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(KubernetesContext(namespace = "default"), "k8s-status")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("kubectl get -n default pods", executor.command)
  }

  @Test
  fun `k8s-rollout accepts resource argument`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(KubernetesContext(namespace = "staging"), "k8s-rollout")

    task.execute(TestEnvironment(executor), projectContext(), listOf("deployment/api"))

    assertEquals("kubectl rollout status -n staging deployment/api", executor.command)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(KubernetesContext(enabled = false), "k8s-apply")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  private fun registerAndGet(ctx: KubernetesContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = KubernetesPlugin()
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
