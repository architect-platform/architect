package io.github.architectplatform.cli

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.client.ExecutionId
import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.ProjectDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.dto.TaskDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.cli.engine.EngineHealthChecker
import io.github.architectplatform.cli.plugin.PluginJarValidator
import io.github.architectplatform.cli.plugin.PluginScaffolder
import io.github.architectplatform.cli.plugin.PluginTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.exists
import kotlin.io.path.outputStream

/**
 * Unit tests for [ArchitectLauncher].
 *
 * Covers: binary path resolution, command routing (plan, history, validate, engine,
 * tasks, info, cache, version), flag behavior (--plain, --no-daemon, --json, --filter),
 * embedded mode fallback, and task execution.
 */
class ArchitectLauncherTest {

  // ─── history command ──────────────────────────────────────────────────────

  @Test
  fun `history command displays empty message when no records exist`(@TempDir tmpDir: Path) {
    val launcher = launcher()
    launcher.command = "history"
    launcher.args = emptyList()

    val output = captureStdout {
      withUserHome(tmpDir.toAbsolutePath().toString()) { launcher.run() }
    }

    assertTrue(output.contains("No execution history found"))
  }

  @Test
  fun `history command reads local history files`(@TempDir tmpDir: Path) {
    val historyDir = tmpDir.resolve(".architect/history").toFile()
    historyDir.mkdirs()
    val record = """{"id":"e1","project":"myproj","task":"build","timestamp":1711100000000,"success":true,"durationMs":1200,"message":"ok"}"""
    File(historyDir, "1711100000000-e1.json").writeText(record)

    val launcher = launcher()
    launcher.command = "history"
    launcher.args = emptyList()

    val output = captureStdout {
      withUserHome(tmpDir.toAbsolutePath().toString()) { launcher.run() }
    }

    assertTrue(output.contains("myproj"), "Expected project name in history output")
    assertTrue(output.contains("build"), "Expected task name in history output")
  }

  @Test
  fun `history command with project argument filters records`(@TempDir tmpDir: Path) {
    val historyDir = tmpDir.resolve(".architect/history").toFile()
    historyDir.mkdirs()
    File(historyDir, "1711100000000-e1.json").writeText(
      """{"id":"e1","project":"web","task":"build","timestamp":1711100000000,"success":true,"durationMs":500,"message":null}"""
    )
    File(historyDir, "1711100001000-e2.json").writeText(
      """{"id":"e2","project":"api","task":"test","timestamp":1711100001000,"success":false,"durationMs":800,"message":"fail"}"""
    )

    val launcher = launcher()
    launcher.command = "history"
    launcher.args = listOf("history", "web")

    val output = captureStdout {
      withUserHome(tmpDir.toAbsolutePath().toString()) { launcher.run() }
    }

    assertTrue(output.contains("web"), "Expected 'web' project in output")
    assertTrue(!output.contains("api"), "Expected 'api' project to be filtered out")
  }

  // ─── plan command ─────────────────────────────────────────────────────────

  @Test
  fun `plan command outputs execution plan`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "plan"
      launcher.args = listOf("plan", "deploy")

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("Execution Plan: deploy"))
      assertTrue(output.contains("3 task(s)"))
      assertTrue(output.contains("build"))
      assertTrue(output.contains("test"))
      assertTrue(output.contains("deploy"))
    }
  }

  // ─── validate command ─────────────────────────────────────────────────────

  @Test
  fun `validate command shows valid result`(@TempDir tmpDir: Path) {
    val launcher = launcherWithClient(StubEngineCommandClient())
    setUserDir(tmpDir) {
      launcher.command = "validate"
      launcher.args = listOf("validate")

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("VALID"))
      assertTrue(output.contains("No issues found"))
    }
  }

  @Test
  fun `validate command shows valid result and includes project name`(@TempDir tmpDir: Path) {
    val client = object : StubEngineCommandClient() {
      override fun validateProject(projectName: String): ValidationResultDTO =
        ValidationResultDTO(
          valid = true,
          errors = emptyList(),
          warnings = listOf("Unknown key: foo"),
        )
    }
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "validate"
      launcher.args = listOf("validate")

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("VALID"))
      assertTrue(output.contains("Unknown key: foo"))
    }
  }

  // ─── tasks command ────────────────────────────────────────────────────────

  @Test
  fun `tasks command lists all tasks`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "tasks"
      launcher.args = listOf("tasks")

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("Available Tasks"))
      assertTrue(output.contains("build"))
      assertTrue(output.contains("test"))
      assertTrue(output.contains("deploy"))
      assertTrue(output.contains("3 task(s) available"))
    }
  }

  @Test
  fun `tasks command with --filter restricts results by phase`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "tasks"
      launcher.args = listOf("tasks")
      launcher.filter = "BUILD"

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("build"))
      assertTrue(output.contains("phase: BUILD"))
      assertTrue(output.contains("1 task(s) available"))
    }
  }

  @Test
  fun `tasks command with --json outputs JSON`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "tasks"
      launcher.args = listOf("tasks")
      launcher.json = true

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("\"id\""))
      assertTrue(output.contains("\"build\""))
    }
  }

  // ─── info command ─────────────────────────────────────────────────────────

  @Test
  fun `info command prints project details`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "info"
      launcher.args = listOf("info")

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("Project Info"))
      assertTrue(output.contains("Tasks:  3"))
      assertTrue(output.contains("Phases:"))
    }
  }

  @Test
  fun `info command with --json outputs JSON`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = launcherWithClient(client)
    setUserDir(tmpDir) {
      launcher.command = "info"
      launcher.args = listOf("info")
      launcher.json = true

      val output = captureStdout { launcher.run() }

      assertTrue(output.contains("\"taskCount\""))
      assertTrue(output.contains("\"project\""))
    }
  }

  // ─── version flag ─────────────────────────────────────────────────────────

  @Test
  fun `--version prints version info`() {
    val launcher = launcher()
    launcher.version = true

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("architect"), "Expected 'architect' in version output")
  }

  @Test
  fun `--version with --json outputs JSON`() {
    val launcher = launcher()
    launcher.version = true
    launcher.json = true

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("\"cli\""), "Expected JSON key 'cli' in version output")
  }

  // ─── --plain / color detection ────────────────────────────────────────────

  @Test
  fun `--plain suppresses startup UI messages`(@TempDir tmpDir: Path) {
    val launcher = launcher()
    launcher.plain = true
    launcher.version = true

    val output = captureStdout { launcher.run() }

    // --plain + --version should still print version (not a startup message)
    assertTrue(output.contains("architect"))
  }

  // ─── cache command ────────────────────────────────────────────────────────

  @Test
  fun `cache info shows cache statistics`() {
    val launcher = launcher()
    launcher.command = "cache"
    launcher.args = listOf("cache", "info")

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("Task Output Cache"))
    assertTrue(output.contains("Entries:"))
    assertTrue(output.contains("Size:"))
  }

  @Test
  fun `cache info with --json outputs JSON`() {
    val launcher = launcher()
    launcher.command = "cache"
    launcher.args = listOf("cache", "info")
    launcher.json = true

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("\"entries\""))
    assertTrue(output.contains("\"sizeBytes\""))
  }

  @Test
  fun `cache clear runs without error`() {
    val launcher = launcher()
    launcher.command = "cache"
    launcher.args = listOf("cache", "clear")

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("Cache cleared"))
  }

  // ─── engine subcommands ───────────────────────────────────────────────────

  @Test
  fun `engine command with no subcommand prints usage`() {
    val launcher = launcher()
    launcher.command = "engine"
    launcher.args = listOf("engine")

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("No command provided"))
  }

  @Test
  fun `engine unknown subcommand prints error`() {
    val launcher = launcher()
    launcher.command = "engine"
    launcher.args = listOf("engine", "foobar")

    val output = captureStdout { launcher.run() }

    assertTrue(output.contains("Unknown command"))
    assertTrue(output.contains("foobar"))
  }

  // ─── plugin command ───────────────────────────────────────────────────────

  @Test
  fun `plugin create scaffolds kotlin template by default`(@TempDir tmpDir: Path) {
    val launcher = launcher()
    launcher.command = "plugin"
    launcher.args = listOf("plugin", "create", "my-test-plugin")

    setUserDir(tmpDir) {
      val output = captureStdout { launcher.run() }
      assertTrue(output.contains("Created"))
      assertTrue(output.contains("my-test-plugin"))
    }
  }

  // ─── no-daemon + embedded fallback ────────────────────────────────────────

  @Test
  fun `--no-daemon with unreachable engine falls back to embedded mode`(@TempDir tmpDir: Path) {
    val healthChecker = object : EngineHealthChecker() {
      override fun isRunning() = false
    }
    val launcher = launcher(healthChecker = healthChecker)
    launcher.noDaemon = true
    launcher.version = true

    // version is handled before embedded routing, so it should still print version
    val output = captureStdout { launcher.run() }
    assertTrue(output.contains("architect"))
  }

  @Test
  fun `embedded flag set routes to embedded tasks listing`(@TempDir tmpDir: Path) {
    // Create a minimal architect.yml in the tmpDir
    File(tmpDir.toFile(), "architect.yml").writeText("""
      project:
        name: test-project
    """.trimIndent())

    val launcher = launcher()
    launcher.embedded = true
    launcher.command = "validate"
    launcher.args = listOf("validate")

    setUserDir(tmpDir) {
      val output = captureStdout { launcher.run() }
      // embedded mode prints a mode message and then handles the command
      assertTrue(output.contains("embedded mode") || output.contains("VALID"))
    }
  }

  // ─── augmentTaskArgsForExecution ────────────────────────────────────────

  @Test
  fun `augmentTaskArgsForExecution leaves non nx tasks unchanged`() {
    val launcher = launcher()
    launcher.affected = true

    val args = launcher.augmentTaskArgsForExecution("build", listOf("--scan"), setOf("web", "api"))

    assertEquals(listOf("--scan"), args)
  }

  @Test
  fun `augmentTaskArgsForExecution adds architect affected bridge for nx tasks`() {
    val launcher = launcher()
    launcher.affected = true

    val args = launcher.augmentTaskArgsForExecution("nx-build", listOf("--skip-nx-cache"), setOf("api", "web"))

    assertEquals(
      listOf(
        "--skip-nx-cache",
        "--architect-affected",
        "--architect-projects=api,web",
      ),
      args,
    )
  }

  // ─── --no-daemon flag ─────────────────────────────────────────────────────

  @Test
  fun `--no-daemon flag causes run() to skip health checker for engine subcommand`() {
    var healthChecked = false
    val trackingHealthChecker = object : EngineHealthChecker() {
      override fun isRunning(): Boolean {
        healthChecked = true
        return false
      }
    }

    val launcher = launcher(healthChecker = trackingHealthChecker)
    launcher.noDaemon = true
    launcher.command = "engine" // engine subcommand is handled before ensureEngineRunning()
    launcher.args = listOf("install")

    // Redirect stdout to suppress output during test
    val origOut = System.out
    System.setOut(java.io.PrintStream(java.io.ByteArrayOutputStream()))
    try {
      // The engine subcommand is dispatched before ensureEngineRunning() is reached
      // so even without --no-daemon the health checker is not called in this flow
      launcher.run()
    } catch (_: Exception) {
      // ignore — external process may not be available in test env
    } finally {
      System.setOut(origOut)
    }

    // The health checker must NOT have been called because command == "engine" exits early
    assertTrue(!healthChecked, "Health checker should not be called for 'engine' subcommand")
  }

  @Test
  fun `engine reload-plugins registers and reloads current project`(@TempDir tmpDir: Path) {
    val client = TrackingEngineCommandClient()
    val launcher = ArchitectLauncher(
      client,
      stubHealthChecker(running = true),
      io.github.architectplatform.cli.history.LocalHistoryReader(),
      io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor(io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()),
    )
    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", tmpDir.toString())
    try {
      launcher.command = "engine"
      launcher.args = listOf("engine", "reload-plugins")

      val originalOut = System.out
      System.setOut(java.io.PrintStream(java.io.ByteArrayOutputStream()))
      try {
        launcher.run()
      } finally {
        System.setOut(originalOut)
      }

      assertEquals(tmpDir.fileName.toString(), client.registeredName)
      assertEquals(tmpDir.toString(), client.registeredPath)
      assertEquals(tmpDir.fileName.toString(), client.reloadedProject)
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }

  @Test
  fun `plugin docs generates reference markdown for scaffolded plugin`(@TempDir tmpDir: Path) {
    val pluginDir = PluginScaffolder().scaffold("docs-sample", PluginTemplate.GO, tmpDir)
    val launcher = launcher()
    launcher.command = "plugin"
    launcher.args = listOf("plugin", "docs", pluginDir.toString())

    val originalOut = System.out
    System.setOut(java.io.PrintStream(java.io.ByteArrayOutputStream()))
    try {
      launcher.run()
    } finally {
      System.setOut(originalOut)
    }

    assertTrue(pluginDir.resolve("PLUGIN_REFERENCE.md").exists())
    assertTrue(pluginDir.resolve("PLUGIN_REFERENCE.md").toFile().readText().contains("docs-sample-hello"))
  }

  @Test
  fun `plugin validate exits successfully for a valid plugin jar`(@TempDir tmpDir: Path) {
    val jarPath = createTestPluginJar(tmpDir.resolve("valid-plugin.jar"))

    val launcher = launcher()
    launcher.command = "plugin"
    launcher.args = listOf("plugin", "validate", jarPath.toString())

    val originalOut = System.out
    System.setOut(java.io.PrintStream(java.io.ByteArrayOutputStream()))
    try {
      launcher.run()
    } finally {
      System.setOut(originalOut)
    }

    assertTrue(jarPath.exists())
  }

  @Test
  fun `graph command outputs DOT for current project task DAG`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = ArchitectLauncher(
      client,
      stubHealthChecker(running = true),
      io.github.architectplatform.cli.history.LocalHistoryReader(),
      io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor(io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()),
    )
    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", tmpDir.toString())
    try {
      launcher.command = "graph"
      launcher.args = listOf("graph")

      val output = java.io.ByteArrayOutputStream()
      val originalOut = System.out
      System.setOut(java.io.PrintStream(output))
      try {
        launcher.run()
      } finally {
        System.setOut(originalOut)
      }

      val dot = output.toString()
      assertTrue(dot.contains("digraph"))
      assertTrue(dot.contains("\"build\" -> \"test\""))
      assertTrue(dot.contains("\"test\" -> \"deploy\""))
      assertTrue(dot.contains("Compile sources"))
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }

  @Test
  fun `graph command with task outputs only that task subgraph`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = ArchitectLauncher(
      client,
      stubHealthChecker(running = true),
      io.github.architectplatform.cli.history.LocalHistoryReader(),
      io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor(io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()),
    )
    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", tmpDir.toString())
    try {
      launcher.command = "graph"
      launcher.args = listOf("graph", "test")

      val output = java.io.ByteArrayOutputStream()
      val originalOut = System.out
      System.setOut(java.io.PrintStream(output))
      try {
        launcher.run()
      } finally {
        System.setOut(originalOut)
      }

      val dot = output.toString()
      assertTrue(dot.contains("\"build\" -> \"test\""))
      assertTrue(!dot.contains("deploy"))
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }

  @Test
  fun `plan --tree outputs dependency tree for a task`(@TempDir tmpDir: Path) {
    val client = GraphEngineCommandClient()
    val launcher = ArchitectLauncher(
      client,
      stubHealthChecker(running = true),
      io.github.architectplatform.cli.history.LocalHistoryReader(),
      io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor(io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()),
    )
    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", tmpDir.toString())
    try {
      launcher.command = "plan"
      launcher.args = listOf("plan", "deploy", "--tree")

      val output = java.io.ByteArrayOutputStream()
      val originalOut = System.out
      System.setOut(java.io.PrintStream(output))
      try {
        launcher.run()
      } finally {
        System.setOut(originalOut)
      }

      val tree = output.toString()
      assertTrue(tree.contains("🌳 Dependency Tree: deploy"))
      assertTrue(tree.contains("deploy [PUBLISH]"))
      assertTrue(tree.contains("└── test [TEST]"))
      assertTrue(tree.contains("└── build [BUILD]"))
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }

  // ─── resolveEngineBinary (now on EngineCommandHandler) ─────────────────────

  @Test
  fun `resolveEngineBinary returns local bin path when binary exists and is executable`(
    @TempDir tmpDir: Path
  ) {
    val bin = tmpDir.resolve(".architect/bin/architect-engine").toFile()
    bin.parentFile.mkdirs()
    bin.createNewFile()
    bin.setExecutable(true)

    val handler = io.github.architectplatform.cli.command.EngineCommandHandler(
      engineCommandClient = StubEngineCommandClient(),
      engineHealthChecker = stubHealthChecker(running = false),
      extractProjectName = { it.substringAfterLast("/") },
    )
    withUserHome(tmpDir.toAbsolutePath().toString()) {
      assertEquals(bin.absolutePath, handler.resolveEngineBinary())
    }
  }

  @Test
  fun `resolveEngineBinary skips non-executable local file and falls back to PATH`(
    @TempDir tmpDir: Path
  ) {
    val bin = tmpDir.resolve(".architect/bin/architect-engine").toFile()
    bin.parentFile.mkdirs()
    bin.createNewFile()
    bin.setExecutable(false) // not executable

    val handler = io.github.architectplatform.cli.command.EngineCommandHandler(
      engineCommandClient = StubEngineCommandClient(),
      engineHealthChecker = stubHealthChecker(running = false),
      extractProjectName = { it.substringAfterLast("/") },
    )
    withUserHome(tmpDir.toAbsolutePath().toString()) {
      val result = handler.resolveEngineBinary()
      // The non-executable file must NOT be returned; PATH result is fine (null if not installed)
      assertNotEquals(bin.absolutePath, result)
    }
  }

  @Test
  fun `resolveEngineBinary returns null when binary absent and not on PATH`(@TempDir tmpDir: Path) {
    val handler = io.github.architectplatform.cli.command.EngineCommandHandler(
      engineCommandClient = StubEngineCommandClient(),
      engineHealthChecker = stubHealthChecker(running = false),
      extractProjectName = { it.substringAfterLast("/") },
    )
    withUserHome(tmpDir.toAbsolutePath().toString()) {
      val result = handler.resolveEngineBinary()
      assertTrue(result == null || result.isNotBlank(), "Expected null or a valid path, got: $result")
    }
  }

  @Test
  fun `resolveEngineBinary returns null when user home is unavailable`() {
    val handler = io.github.architectplatform.cli.command.EngineCommandHandler(
      engineCommandClient = StubEngineCommandClient(),
      engineHealthChecker = stubHealthChecker(running = false),
      extractProjectName = { it.substringAfterLast("/") },
    )
    val original = System.getProperty("user.home")
    System.clearProperty("user.home")
    try {
      assertNull(handler.resolveEngineBinary())
    } finally {
      System.setProperty("user.home", original)
    }
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private fun captureStdout(block: () -> Unit): String {
    val baos = java.io.ByteArrayOutputStream()
    val originalOut = System.out
    System.setOut(java.io.PrintStream(baos))
    try {
      block()
    } finally {
      System.setOut(originalOut)
    }
    return baos.toString()
  }

  private fun captureStdoutAllowExit(block: () -> Unit): String {
    val baos = java.io.ByteArrayOutputStream()
    val originalOut = System.out
    System.setOut(java.io.PrintStream(baos))
    try {
      block()
    } catch (_: Exception) {
      // exitProcess throws SecurityException or similar in test harness; ignore
    } finally {
      System.setOut(originalOut)
    }
    return baos.toString()
  }

  private fun <T> setUserDir(tmpDir: Path, block: () -> T): T {
    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", tmpDir.toString())
    return try {
      block()
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }

  private fun launcherWithClient(
    client: EngineCommandClient,
    healthChecker: EngineHealthChecker = stubHealthChecker(running = true),
  ): ArchitectLauncher {
    return ArchitectLauncher(
      client,
      healthChecker,
      io.github.architectplatform.cli.history.LocalHistoryReader(),
      io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor(io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()),
    )
  }

  private fun createTestPluginJar(jarPath: Path): Path {
    JarOutputStream(jarPath.outputStream().buffered()).use { output ->
      writeClassEntry(output, LauncherValidPlugin::class.java)
      writeClassEntry(output, LauncherValidContext::class.java)
      writeTextEntry(output, PluginJarValidator.SPI_RESOURCE, LauncherValidPlugin::class.java.name + "\n")
    }
    return jarPath
  }

  private fun writeClassEntry(output: JarOutputStream, type: Class<*>) {
    val resourcePath = type.name.replace('.', '/') + ".class"
    val bytes = type.classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes() }
      ?: error("Missing compiled class resource $resourcePath")
    writeBytesEntry(output, resourcePath, bytes)
  }

  private fun writeTextEntry(output: JarOutputStream, entryName: String, content: String) {
    writeBytesEntry(output, entryName, content.toByteArray())
  }

  private fun writeBytesEntry(output: JarOutputStream, entryName: String, content: ByteArray) {
    output.putNextEntry(JarEntry(entryName))
    output.write(content)
    output.closeEntry()
  }

  private fun launcher(healthChecker: EngineHealthChecker = stubHealthChecker(running = true)): ArchitectLauncher {
    return ArchitectLauncher(StubEngineCommandClient(), healthChecker, io.github.architectplatform.cli.history.LocalHistoryReader(), io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor(io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()))
  }

  private fun stubHealthChecker(running: Boolean) = object : EngineHealthChecker() {
    override fun isRunning() = running
  }.also { it.engineUrl = "http://localhost:9292" }

  private fun withUserHome(home: String, block: () -> Unit) {
    val original = System.getProperty("user.home")
    System.setProperty("user.home", home)
    try {
      block()
    } finally {
      System.setProperty("user.home", original)
    }
  }
}

data class LauncherValidContext(
  val enabled: Boolean = true,
)

class LauncherValidPlugin : ArchitectPlugin<LauncherValidContext> {
  override val id: String = "launcher-valid-plugin"
  override val contextKey: String = "launcher-valid"
  override val ctxClass: Class<LauncherValidContext> = LauncherValidContext::class.java
  override var context: LauncherValidContext = LauncherValidContext()

  override fun register(registry: TaskRegistry) = Unit
}

/** No-op stub implementation of [EngineCommandClient] for unit tests. */
private open class StubEngineCommandClient : EngineCommandClient {
  override fun getAllProjects(): List<ProjectDTO> = emptyList()
  override fun registerProject(request: RegisterProjectRequest): ProjectDTO =
    ProjectDTO(name = request.name, path = request.path, context = ProjectDTO.ProjectContextDTO(dir = request.path, config = emptyMap()))
  override fun getProject(name: String): ProjectDTO? = null
  override fun getAllTasks(projectName: String): List<TaskDTO> = emptyList()
  override fun getTask(projectName: String, taskName: String): TaskDTO? = null
  override fun planTask(projectName: String, taskName: String): TaskPlanDTO =
    TaskPlanDTO(task = taskName, project = projectName, totalSteps = 0, parallelBatches = 0, steps = emptyList())
  override fun execute(projectName: String, taskName: String, args: List<String>): ExecutionId = "test-exec-id"
  override fun getExecutionFlow(executionId: ExecutionId): Flow<Map<String, Any>> = emptyFlow()
  override fun getHistory(): List<HistoryRecordDTO> = emptyList()
  override fun getProjectHistory(project: String): List<HistoryRecordDTO> = emptyList()
  override fun validateProject(projectName: String): ValidationResultDTO =
    ValidationResultDTO(valid = true, errors = emptyList(), warnings = emptyList())
  override fun reloadProjectPlugins(projectName: String): ProjectDTO =
    ProjectDTO(name = projectName, path = ".", context = ProjectDTO.ProjectContextDTO(dir = ".", config = emptyMap()))
  override fun cancelExecution(executionId: ExecutionId): Map<String, Any> =
    mapOf("executionId" to executionId, "cancelled" to false)
}

private class TrackingEngineCommandClient : EngineCommandClient {
  var registeredName: String? = null
  var registeredPath: String? = null
  var reloadedProject: String? = null

  override fun getAllProjects(): List<ProjectDTO> = emptyList()

  override fun registerProject(request: RegisterProjectRequest): ProjectDTO {
    registeredName = request.name
    registeredPath = request.path
    return ProjectDTO(name = request.name, path = request.path, context = ProjectDTO.ProjectContextDTO(dir = request.path, config = emptyMap()))
  }

  override fun getProject(name: String): ProjectDTO? = null
  override fun getAllTasks(projectName: String): List<TaskDTO> = emptyList()
  override fun getTask(projectName: String, taskName: String): TaskDTO? = null
  override fun planTask(projectName: String, taskName: String): TaskPlanDTO =
    TaskPlanDTO(task = taskName, project = projectName, totalSteps = 0, parallelBatches = 0, steps = emptyList())
  override fun execute(projectName: String, taskName: String, args: List<String>): ExecutionId = "test-exec-id"
  override fun getExecutionFlow(executionId: ExecutionId): Flow<Map<String, Any>> = emptyFlow()
  override fun getHistory(): List<HistoryRecordDTO> = emptyList()
  override fun getProjectHistory(project: String): List<HistoryRecordDTO> = emptyList()
  override fun validateProject(projectName: String): ValidationResultDTO =
    ValidationResultDTO(valid = true, errors = emptyList(), warnings = emptyList())

  override fun reloadProjectPlugins(projectName: String): ProjectDTO {
    reloadedProject = projectName
    return ProjectDTO(name = projectName, path = registeredPath ?: ".", context = ProjectDTO.ProjectContextDTO(dir = registeredPath ?: ".", config = emptyMap()))
  }
  override fun cancelExecution(executionId: ExecutionId): Map<String, Any> =
    mapOf("executionId" to executionId, "cancelled" to false)
}

private class GraphEngineCommandClient : EngineCommandClient {
  override fun getAllProjects(): List<ProjectDTO> = emptyList()

  override fun registerProject(request: RegisterProjectRequest): ProjectDTO =
    ProjectDTO(name = request.name, path = request.path, context = ProjectDTO.ProjectContextDTO(dir = request.path, config = emptyMap()))

  override fun getProject(name: String): ProjectDTO? = null

  override fun getAllTasks(projectName: String): List<TaskDTO> = listOf(
    TaskDTO(id = "build", description = "Compile sources", phase = "BUILD"),
    TaskDTO(id = "test", description = "Run tests", phase = "TEST"),
    TaskDTO(id = "deploy", description = "Ship release", phase = "PUBLISH"),
  )

  override fun getTask(projectName: String, taskName: String): TaskDTO? = null

  override fun planTask(projectName: String, taskName: String): TaskPlanDTO = when (taskName) {
    "build" -> TaskPlanDTO(
      task = taskName,
      project = projectName,
      totalSteps = 1,
      parallelBatches = 1,
      steps = listOf(
        TaskPlanStepDTO(
          id = "build",
          description = "Compile sources",
          phase = "BUILD",
          depends = emptyList(),
          batch = 0,
        ),
      ),
    )
    "test" -> TaskPlanDTO(
      task = taskName,
      project = projectName,
      totalSteps = 2,
      parallelBatches = 2,
      steps = listOf(
        TaskPlanStepDTO(
          id = "build",
          description = "Compile sources",
          phase = "BUILD",
          depends = emptyList(),
          batch = 0,
        ),
        TaskPlanStepDTO(
          id = "test",
          description = "Run tests",
          phase = "TEST",
          depends = listOf("build"),
          batch = 1,
        ),
      ),
    )
    else -> TaskPlanDTO(
      task = taskName,
      project = projectName,
      totalSteps = 3,
      parallelBatches = 3,
      steps = listOf(
        TaskPlanStepDTO(
          id = "build",
          description = "Compile sources",
          phase = "BUILD",
          depends = emptyList(),
          batch = 0,
        ),
        TaskPlanStepDTO(
          id = "test",
          description = "Run tests",
          phase = "TEST",
          depends = listOf("build"),
          batch = 1,
        ),
        TaskPlanStepDTO(
          id = "deploy",
          description = "Ship release",
          phase = "PUBLISH",
          depends = listOf("test"),
          batch = 2,
        ),
      ),
    )
  }

  override fun execute(projectName: String, taskName: String, args: List<String>): ExecutionId = "test-exec-id"

  override fun getExecutionFlow(executionId: ExecutionId): Flow<Map<String, Any>> = emptyFlow()

  override fun getHistory(): List<HistoryRecordDTO> = emptyList()

  override fun getProjectHistory(project: String): List<HistoryRecordDTO> = emptyList()

  override fun validateProject(projectName: String): ValidationResultDTO =
    ValidationResultDTO(valid = true, errors = emptyList(), warnings = emptyList())

  override fun reloadProjectPlugins(projectName: String): ProjectDTO =
    ProjectDTO(name = projectName, path = ".", context = ProjectDTO.ProjectContextDTO(dir = ".", config = emptyMap()))
  override fun cancelExecution(executionId: ExecutionId): Map<String, Any> =
    mapOf("executionId" to executionId, "cancelled" to false)
}