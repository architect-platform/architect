package io.github.architectplatform.plugins.git

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class GitPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = GitPlugin()
    assertEquals("git-plugin", plugin.id)
    assertEquals("git", plugin.contextKey)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = GitPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("git-config" in ids)
    assertTrue("git-remote" in ids)
    assertTrue("git-status" in ids)
    assertTrue("git-add" in ids)
    assertTrue("git-commit" in ids)
    assertTrue("git-push" in ids)
    assertTrue("git-pull" in ids)
    assertTrue("git-tag" in ids)
    assertEquals(16, ids.size)
  }

  @Test
  fun `git-status executes git status command`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GitContext(), "git-status")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("git status ", executor.command)
  }

  @Test
  fun `git-commit passes escaped arguments`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GitContext(), "git-commit")

    task.execute(TestEnvironment(executor), projectContext(), listOf("-m", "initial commit"))

    assertTrue(executor.command!!.contains("git commit"))
    assertTrue(executor.command!!.contains("'-m'"))
  }

  @Test
  fun `git-config applies configuration entries`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GitContext(config = mapOf("user.name" to "Test User")), "git-config")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertTrue(executor.commands.any { it.contains("git config --local") && it.contains("user.name") })
  }

  @Test
  fun `git-config skips invalid keys`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GitContext(config = mapOf("invalid key!" to "value")), "git-config")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertFalse(result.success)
    assertTrue(executor.commands.isEmpty())
  }

  @Test
  fun `git-config returns success when no config specified`() {
    val task = registerAndGet(GitContext(config = emptyMap()), "git-config")

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), projectContext(), emptyList())

    assertTrue(result.success)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GitContext(enabled = false), "git-status")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertTrue(executor.commands.isEmpty())
  }

  @Test
  fun `git-push executes in project directory`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GitContext(), "git-push")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("git push ", executor.command)
  }

  private fun registerAndGet(ctx: GitContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = GitPlugin()
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
    val commands = mutableListOf<String>()
    var workingDir: String? = null
    override fun execute(command: String, workingDir: String?) {
      this.command = command
      this.commands.add(command)
      this.workingDir = workingDir
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
