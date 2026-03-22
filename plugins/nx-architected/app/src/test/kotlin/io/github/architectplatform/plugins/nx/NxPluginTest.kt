package io.github.architectplatform.plugins.nx

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class NxPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = NxPlugin()
    assertEquals("nx-plugin", plugin.id)
    assertEquals("nx", plugin.contextKey)
  }

  @Test
  fun `default context`() {
    val ctx = NxContext()
    assertTrue(ctx.targets.isEmpty())
    assertEquals(3, ctx.parallel)
    assertFalse(ctx.nxCloud)
    assertFalse(ctx.affected)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `registers default targets`() {
    val plugin = NxPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("nx-build" in ids)
    assertTrue("nx-test" in ids)
    assertTrue("nx-lint" in ids)
    assertTrue("nx-e2e" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `registers custom targets from context`() {
    val plugin = NxPlugin()
    plugin.init(NxContext(targets = listOf("storybook", "serve")))
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    // default 4 + 2 custom
    assertTrue("nx-storybook" in ids)
    assertTrue("nx-serve" in ids)
    assertEquals(6, ids.size)
  }

  @Test
  fun `custom targets that overlap default are not duplicated`() {
    val plugin = NxPlugin()
    plugin.init(NxContext(targets = listOf("build", "deploy")))
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    // build is default, deploy is custom → 4 default + 1 custom
    assertEquals(5, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = NxPlugin()
    plugin.init(NxContext(affected = true, parallel = 8, nxCloud = true))
    assertTrue(plugin.context.affected)
    assertEquals(8, plugin.context.parallel)
    assertTrue(plugin.context.nxCloud)
  }

  @Test
  fun `task uses architect affected bridge projects when provided`() {
    val task = NxTask(
      id = "nx-build",
      nxTarget = "build",
      phase = CoreWorkflow.BUILD,
      ctx = NxContext(parallel = 5),
    )
    val commandExecutor = RecordingCommandExecutor()

    val result = task.execute(
      environment = TestEnvironment(commandExecutor),
      projectContext = ProjectContext(Path.of("/repo"), emptyMap()),
      args = listOf(
        "--architect-affected",
        "--architect-projects=apps-api,apps-web",
        "--skip-nx-cache",
      ),
    )

    assertTrue(result.success)
    assertEquals(
      "npx nx run-many --target=build --projects=apps-api,apps-web --parallel=5 --skip-nx-cache",
      commandExecutor.command,
    )
    assertEquals("/repo", commandExecutor.workingDir)
  }

  @Test
  fun `task falls back to nx affected when no project list is provided`() {
    val task = NxTask(
      id = "nx-test",
      nxTarget = "test",
      phase = CoreWorkflow.TEST,
      ctx = NxContext(),
    )
    val commandExecutor = RecordingCommandExecutor()

    val result = task.execute(
      environment = TestEnvironment(commandExecutor),
      projectContext = ProjectContext(Path.of("/repo"), emptyMap()),
      args = listOf("--architect-affected", "--configuration=ci"),
    )

    assertTrue(result.success)
    assertEquals(
      "npx nx affected --target=test --parallel=3 --configuration=ci",
      commandExecutor.command,
    )
  }

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

  private class TestEnvironment(
    private val commandExecutor: CommandExecutor,
  ) : Environment {
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
