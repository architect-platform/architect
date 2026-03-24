package io.github.architectplatform.plugins.maven

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

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

  @Test
  fun `mvn-verify executes with profiles and settings`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(MavenContext(profiles = listOf("release", "ci"), settings = "ci-settings.xml"), "mvn-verify")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("mvn verify -P 'release,ci' -s 'ci-settings.xml'", executor.command)
  }

  @Test
  fun `mvn-package includes skipTests flag`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(MavenContext(skipTests = true), "mvn-package")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("mvn package -DskipTests", executor.command)
  }

  @Test
  fun `mvn-deploy executes with all flags`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(MavenContext(skipTests = true, profiles = listOf("release")), "mvn-deploy")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("mvn deploy -DskipTests -P 'release'", executor.command)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(MavenContext(enabled = false), "mvn-verify")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  private fun registerAndGet(ctx: MavenContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = MavenPlugin()
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
