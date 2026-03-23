package io.github.architectplatform.plugins.rust

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class RustPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = RustPlugin()
    assertEquals("rust-plugin", plugin.id)
    assertEquals("rust", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = RustContext()
    assertEquals("release", ctx.profile)
    assertTrue(ctx.features.isEmpty())
    assertEquals("", ctx.target)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = RustPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("cargo-build" in ids)
    assertTrue("cargo-test" in ids)
    assertTrue("cargo-lint" in ids)
    assertTrue("cargo-publish" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = RustPlugin()
    plugin.init(RustContext(profile = "debug", features = listOf("serde", "tokio"), target = "x86_64-unknown-linux-gnu"))
    assertEquals("debug", plugin.context.profile)
    assertEquals(listOf("serde", "tokio"), plugin.context.features)
    assertEquals("x86_64-unknown-linux-gnu", plugin.context.target)
  }

  @Test
  fun `cargo-build executes with profile and features`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(RustContext(profile = "debug", features = listOf("serde", "tokio")), "cargo-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("cargo build --debug --features serde,tokio", executor.command)
  }

  @Test
  fun `cargo-build includes target flag`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(RustContext(target = "aarch64-apple-darwin"), "cargo-build")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(executor.command!!.contains("--target aarch64-apple-darwin"))
  }

  @Test
  fun `cargo-test executes with features`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(RustContext(features = listOf("full")), "cargo-test")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("cargo test --features full", executor.command)
  }

  @Test
  fun `cargo-lint uses clippy with warnings denied`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(RustContext(), "cargo-lint")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("cargo clippy -- -D warnings", executor.command)
  }

  @Test
  fun `disabled task skips execution`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(RustContext(enabled = false), "cargo-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertNull(executor.command)
  }

  private fun registerAndGet(ctx: RustContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = RustPlugin()
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
