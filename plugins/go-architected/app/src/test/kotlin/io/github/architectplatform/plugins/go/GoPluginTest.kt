package io.github.architectplatform.plugins.go

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class GoPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = GoPlugin()
    assertEquals("go-plugin", plugin.id)
    assertEquals("go", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = GoContext()
    assertEquals("", ctx.module)
    assertEquals("", ctx.ldflags)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = GoPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("go-build" in ids)
    assertTrue("go-test" in ids)
    assertTrue("go-lint" in ids)
    assertTrue("go-release" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = GoPlugin()
    plugin.init(GoContext(module = "github.com/org/repo", ldflags = "-s -w"))
    assertEquals("github.com/org/repo", plugin.context.module)
    assertEquals("-s -w", plugin.context.ldflags)
  }

  @Test
  fun `go-build executes with default flags`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GoContext(), "go-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("go build ./...", executor.command)
  }

  @Test
  fun `go-build includes ldflags and output binary`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GoContext(ldflags = "-s -w", outputBinary = "myapp"), "go-build")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("go build -ldflags '-s -w' -o myapp ./...", executor.command)
  }

  @Test
  fun `go-test executes with default target`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GoContext(), "go-test")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("go test ./...", executor.command)
  }

  @Test
  fun `go-lint runs golangci-lint`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GoContext(), "go-lint")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("golangci-lint run", executor.command)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(GoContext(enabled = false), "go-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  private fun registerAndGet(ctx: GoContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = GoPlugin()
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
