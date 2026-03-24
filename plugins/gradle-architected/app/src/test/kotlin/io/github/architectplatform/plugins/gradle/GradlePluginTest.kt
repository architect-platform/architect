package io.github.architectplatform.plugins.gradle

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class GradlePluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = GradlePlugin()
    assertEquals("gradle-plugin", plugin.id)
    assertEquals("gradle", plugin.contextKey)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = GradlePlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("gradle-" in ids)
    assertTrue("gradle-build" in ids)
    assertTrue("gradle-test" in ids)
    assertTrue("gradle-run" in ids)
    assertTrue("gradle-publishGprPublicationToGitHubPackagesRepository" in ids)
    assertEquals(5, ids.size)
  }

  @Test
  fun `registers publish task when github package release is enabled`() {
    val plugin = GradlePlugin()
    plugin.init(GradleContext(projects = listOf(GradleProjectContext(name = "api", githubPackageRelease = true))))
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("gradle-publishGprPublicationToGitHubPackagesRepository" in ids)
  }

  @Test
  fun `gradle-build executes gradlew for each project`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(
      GradleContext(projects = listOf(GradleProjectContext(name = "api", path = "api"))),
      "gradle-build"
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(Path.of("/repo"), emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("./gradlew build ", executor.commands[0].first)
    assertTrue(executor.commands[0].second!!.endsWith("api"))
  }

  @Test
  fun `gradle-build aggregates results from multiple projects`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(
      GradleContext(projects = listOf(
        GradleProjectContext(name = "api", path = "api"),
        GradleProjectContext(name = "engine", path = "engine")
      )),
      "gradle-build"
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(Path.of("/repo"), emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals(2, executor.commands.size)
  }

  @Test
  fun `publish task skips project without githubPackageRelease`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(
      GradleContext(projects = listOf(GradleProjectContext(name = "api", path = "api", githubPackageRelease = false))),
      "gradle-publishGprPublicationToGitHubPackagesRepository"
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(Path.of("/repo"), emptyMap()), emptyList())

    assertTrue(result.success)
    assertTrue(executor.commands.isEmpty())
  }

  @Test
  fun `gradle-build returns failure on executor exception`() {
    val task = registerAndGet(
      GradleContext(projects = listOf(GradleProjectContext(name = "api", path = "api"))),
      "gradle-build"
    )

    val result = task.execute(TestEnvironment(FailingCommandExecutor()), ProjectContext(Path.of("/repo"), emptyMap()), emptyList())

    assertFalse(result.success)
  }

  @Test
  fun `gradle-build rejects project path traversal`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(
      GradleContext(projects = listOf(GradleProjectContext(name = "api", path = "../outside"))),
      "gradle-build"
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(Path.of("/repo"), emptyMap()), emptyList())

    assertFalse(result.success)
    assertTrue(executor.commands.isEmpty())
    assertTrue(result.message!!.contains("invalid project path"))
  }

  private fun registerAndGet(ctx: GradleContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = GradlePlugin()
    plugin.init(ctx)
    val registry = TestTaskRegistry()
    plugin.register(registry)
    return registry.get(taskId)!!
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }

  private class RecordingCommandExecutor : CommandExecutor {
    val commands = mutableListOf<Pair<String, String?>>()
    override fun execute(command: String, workingDir: String?) {
      commands.add(command to workingDir)
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
      throw IllegalArgumentException("Unsupported service: ${type.name}")
    }
    override fun publish(event: Any) {}
  }
}
