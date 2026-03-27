package io.github.architectplatform.plugins.javascript

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.testing.ArchitectPluginTestKit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Files

class JavaScriptPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = JavaScriptPlugin()
    assertEquals("javascript-plugin", plugin.id)
    assertEquals("javascript", plugin.contextKey)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = JavaScriptPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("javascript-install" in ids)
    assertTrue("javascript-workspace-check" in ids)
    assertTrue("javascript-lockfile-check" in ids)
    assertTrue("javascript-audit" in ids)
    assertTrue("javascript-build" in ids)
    assertTrue("javascript-test" in ids)
    assertTrue("javascript-lint" in ids)
    assertTrue("javascript-dev" in ids)
    assertTrue("javascript-version" in ids)
    assertTrue("javascript-publish" in ids)
    assertEquals(10, ids.size)
  }

  @Test
  fun `javascript-install uses npm by default`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-install")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertTrue(result.success)
    assertEquals("npm install", executor.command)
  }

  @Test
  fun `javascript-build uses yarn when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "yarn"), "javascript-build")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("yarn build", executor.command)
  }

  @Test
  fun `javascript-test uses pnpm when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "pnpm"), "javascript-test")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("pnpm test", executor.command)
  }

  @Test
  fun `javascript-lint uses npm run lint`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-lint")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("npm run lint", executor.command)
  }

  @Test
  fun `javascript-dev uses pnpm dev`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "pnpm"), "javascript-dev")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("pnpm dev", executor.command)
  }

  @Test
  fun `javascript-build uses bun run build when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "bun"), "javascript-build")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("bun run build", executor.command)
  }

  @Test
  fun `javascript-install uses yarn berry immutable install when configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "yarn", yarnMode = "berry"), "javascript-install")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("yarn install --immutable", executor.command)
  }

  @Test
  fun `task passes extra args`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-test")

    task.execute(TestEnvironment(executor), projectContext(), listOf("--coverage"))

    assertEquals("npm test '--coverage'", executor.command)
  }

  @Test
  fun `task returns failure on executor exception`() {
    val task = registerAndGet(JavaScriptContext(), "javascript-build")

    val result = task.execute(TestEnvironment(FailingCommandExecutor()), projectContext(), emptyList())

    assertFalse(result.success)
  }

  @Test
  fun `workspace check detects single package project`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(), "javascript-workspace-check")
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample"}""")

    val result = task.execute(TestEnvironment(executor), ProjectContext(projectDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("single-package", result.data["workspaceType"])
    assertNull(executor.command)
  }

  @Test
  fun `workspace check detects pnpm workspace`() {
    val task = registerAndGet(JavaScriptContext(), "javascript-workspace-check")
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample"}""")
    Files.writeString(projectDir.resolve("pnpm-workspace.yaml"), "packages:\n  - packages/*\n")

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(projectDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("pnpm-workspace", result.data["workspaceType"])
  }

  @Test
  fun `lockfile check fails when lockfile is missing`() {
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-lockfile-check")
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample"}""")

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(projectDir, emptyMap()), emptyList())

    assertFalse(result.success)
    assertTrue(result.message!!.contains("package-lock.json"))
  }

  @Test
  fun `lockfile check passes when lockfile exists`() {
    val task = registerAndGet(JavaScriptContext(packageManager = "pnpm"), "javascript-lockfile-check")
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample"}""")
    Files.writeString(projectDir.resolve("pnpm-lock.yaml"), "lockfileVersion: 9.0\n")

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(projectDir, emptyMap()), emptyList())

    assertTrue(result.success)
  }

  @Test
  fun `javascript-audit uses npm audit`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-audit")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("npm audit", executor.command)
  }

  @Test
  fun `javascript-version uses default bump from context`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm", defaultVersionBump = "minor"), "javascript-version")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("npm version minor", executor.command)
  }

  @Test
  fun `javascript-version uses arg bump when provided`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "pnpm"), "javascript-version")

    task.execute(TestEnvironment(executor), projectContext(), listOf("major"))

    assertEquals("pnpm version major", executor.command)
  }

  @Test
  fun `javascript-publish uses configured access for npm`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm", publishAccess = "restricted"), "javascript-publish")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("npm publish --access restricted", executor.command)
  }

  @Test
  fun `javascript-version forwards additional args after bump`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "npm"), "javascript-version")

    task.execute(TestEnvironment(executor), projectContext(), listOf("minor", "--no-git-tag-version"))

    assertEquals("npm version minor '--no-git-tag-version'", executor.command)
  }

  @Test
  fun `workspace-check identifies package json workspaces`() {
    val task = JavaScriptTask("workspace-check", NoPhase, JavaScriptContext())
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample","workspaces":["packages/*"]}""")

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(projectDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("package-json-workspaces", result.data["workspaceType"])
  }

  @Test
  fun `workspace-check identifies nx workspace`() {
    val task = JavaScriptTask("workspace-check", NoPhase, JavaScriptContext())
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample"}""")
    Files.writeString(projectDir.resolve("nx.json"), """{"npmScope":"sample"}""")

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(projectDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("nx", result.data["workspaceType"])
  }

  @Test
  fun `lockfile check validates bun lockfile`() {
    val task = JavaScriptTask("lockfile-check", NoPhase, JavaScriptContext(packageManager = "bun"))
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample"}""")
    Files.write(projectDir.resolve("bun.lockb"), byteArrayOf(1, 2, 3))

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(projectDir, emptyMap()), emptyList())

    assertTrue(result.success)
  }

  @Test
  fun `javascript-publish uses bun publish command`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(packageManager = "bun"), "javascript-publish")

    task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertEquals("bun publish", executor.command)
  }

  @Test
  fun `plugin test kit executable tasks pass for default config`() {
    val projectDir = tempProject()
    Files.writeString(projectDir.resolve("package.json"), """{"name":"sample","version":"1.0.0"}""")
    Files.writeString(projectDir.resolve("package-lock.json"), "{}")

    val executor = RecordingCommandExecutor()
    val kit = ArchitectPluginTestKit(JavaScriptPlugin(), projectDir = projectDir)
      .configure(JavaScriptContext())
      .withService(CommandExecutor::class.java, executor)

    val installResult = kit.executeTask("javascript-install")
    val lockfileResult = kit.executeTask("javascript-lockfile-check")
    val workspaceResult = kit.executeTask("javascript-workspace-check")

    assertTrue(installResult.success)
    assertTrue(lockfileResult.success)
    assertTrue(workspaceResult.success)
  }

  @Test
  fun `task rejects working directory traversal`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(JavaScriptContext(workingDirectory = "../outside"), "javascript-build")

    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())

    assertFalse(result.success)
    assertNull(executor.command)
    assertTrue(result.message!!.contains("invalid working directory"))
  }

  private fun registerAndGet(ctx: JavaScriptContext, taskId: String): io.github.architectplatform.api.core.tasks.Task {
    val plugin = JavaScriptPlugin()
    plugin.init(ctx)
    val registry = TestTaskRegistry()
    plugin.register(registry)
    return registry.get(taskId)!!
  }

  private fun projectContext() = ProjectContext(Path.of("/repo"), emptyMap())

  private fun tempProject(): Path {
    return Files.createTempDirectory("js-plugin-test-")
  }

  private object NoPhase : Phase {
    override val id: String = "test"
    override fun description(): String = "test phase"
    override fun parent(): Phase? = null
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
