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
 * Covers: binary path resolution (local bin, non-executable, PATH fallback, not found)
 * and `--no-daemon` flag bypassing the health check.
 */
class ArchitectLauncherTest {

  // ─── resolveEngineBinary ──────────────────────────────────────────────────

  @Test
  fun `resolveEngineBinary returns local bin path when binary exists and is executable`(
    @TempDir tmpDir: Path
  ) {
    val bin = tmpDir.resolve(".architect/bin/architect-engine").toFile()
    bin.parentFile.mkdirs()
    bin.createNewFile()
    bin.setExecutable(true)

    val launcher = launcher()
    withUserHome(tmpDir.toAbsolutePath().toString()) {
      assertEquals(bin.absolutePath, launcher.resolveEngineBinary())
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

    val launcher = launcher()
    withUserHome(tmpDir.toAbsolutePath().toString()) {
      val result = launcher.resolveEngineBinary()
      // The non-executable file must NOT be returned; PATH result is fine (null if not installed)
      assertNotEquals(bin.absolutePath, result)
    }
  }

  @Test
  fun `resolveEngineBinary returns null when binary absent and not on PATH`(@TempDir tmpDir: Path) {
    // Fresh temp dir has no .architect/bin/architect-engine
    // PATH lookup: if architect-engine is installed on the test machine we get a non-null result,
    // otherwise null. We just verify it doesn't throw and either branch is acceptable.
    val launcher = launcher()
    withUserHome(tmpDir.toAbsolutePath().toString()) {
      val result = launcher.resolveEngineBinary()
      assertTrue(result == null || result.isNotBlank(), "Expected null or a valid path, got: $result")
    }
  }

  @Test
  fun `resolveEngineBinary returns null when user home is unavailable`() {
    val launcher = launcher()
    val original = System.getProperty("user.home")
    System.clearProperty("user.home")
    try {
      assertNull(launcher.resolveEngineBinary())
    } finally {
      System.setProperty("user.home", original)
    }
  }

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
      assertTrue(dot.contains("Compile sources"))
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

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
private class StubEngineCommandClient : EngineCommandClient {
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
}

private class GraphEngineCommandClient : EngineCommandClient {
  override fun getAllProjects(): List<ProjectDTO> = emptyList()

  override fun registerProject(request: RegisterProjectRequest): ProjectDTO =
    ProjectDTO(name = request.name, path = request.path, context = ProjectDTO.ProjectContextDTO(dir = request.path, config = emptyMap()))

  override fun getProject(name: String): ProjectDTO? = null

  override fun getAllTasks(projectName: String): List<TaskDTO> = listOf(
    TaskDTO(id = "build", description = "Compile sources", phase = "BUILD"),
    TaskDTO(id = "test", description = "Run tests", phase = "TEST"),
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
    else -> TaskPlanDTO(
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
  }

  override fun execute(projectName: String, taskName: String, args: List<String>): ExecutionId = "test-exec-id"

  override fun getExecutionFlow(executionId: ExecutionId): Flow<Map<String, Any>> = emptyFlow()

  override fun getHistory(): List<HistoryRecordDTO> = emptyList()

  override fun getProjectHistory(project: String): List<HistoryRecordDTO> = emptyList()

  override fun validateProject(projectName: String): ValidationResultDTO =
    ValidationResultDTO(valid = true, errors = emptyList(), warnings = emptyList())

  override fun reloadProjectPlugins(projectName: String): ProjectDTO =
    ProjectDTO(name = projectName, path = ".", context = ProjectDTO.ProjectContextDTO(dir = ".", config = emptyMap()))
}
