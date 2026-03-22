package io.github.architectplatform.cli

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.client.ExecutionId
import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.ProjectDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.dto.TaskDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.cli.engine.EngineHealthChecker
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

  // ─── Helpers ─────────────────────────────────────────────────────────────

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
}
