package io.github.architectplatform.plugins.docker

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

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

  @Test
  fun `docker-build executes correct command with image and build args`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(DockerContext(image = "myapp:latest", buildArgs = mapOf("ENV" to "prod")), "docker-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("docker build --build-arg ENV=prod -t myapp:latest -f Dockerfile .", executor.command)
  }

  @Test
  fun `docker-build includes platform flag`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(DockerContext(image = "myapp", platforms = listOf("linux/amd64", "linux/arm64")), "docker-build")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(executor.command!!.contains("--platform linux/amd64,linux/arm64"))
  }

  @Test
  fun `docker-push executes correct command`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(DockerContext(image = "myapp:v1"), "docker-push")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("docker push myapp:v1", executor.command)
  }

  @Test
  fun `docker-compose-up uses configured compose file`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(DockerContext(composeFile = "compose.prod.yml"), "docker-compose-up")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("docker compose -f compose.prod.yml up -d", executor.command)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(DockerContext(enabled = false), "docker-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  @Test
  fun `docker-build returns failure on executor exception`() {
    val task = registerAndGet(DockerContext(image = "myapp"), "docker-build")

    val result = task.execute(TestEnvironment(FailingCommandExecutor()), projectContext(), emptyList())

    assertFalse(result.success)
  }

  private fun registerAndGet(ctx: DockerContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = DockerPlugin()
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

  private class FailingCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {
      throw RuntimeException("Command failed")
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
