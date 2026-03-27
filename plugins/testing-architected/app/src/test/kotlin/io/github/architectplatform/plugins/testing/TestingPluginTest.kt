package io.github.architectplatform.plugins.testing

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.testing.ArchitectPluginTestKit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TestingPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = TestingPlugin()
    assertEquals("testing-plugin", plugin.id)
    assertEquals("testing", plugin.contextKey)
    assertEquals(TestingContext::class.java, plugin.ctxClass)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = TestingPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertEquals(setOf("test-unit", "test-integration", "test-e2e", "test-coverage"), ids)
  }

  @Test
  fun `config schema is provided`() {
    val schema = TestingPlugin().configSchema()
    assertNotNull(schema)
    assertEquals("object", schema["type"])
  }

  @Test
  fun `detects junit from gradle project`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { jacoco }\n")
    assertEquals(TestingTask.DetectedFramework.JUNIT, TestingTask.detectFramework(tempDir))
  }

  @Test
  fun `detects pytest from pyproject`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("pyproject.toml"), "[tool.pytest.ini_options]\naddopts = \"-q\"\n")
    assertEquals(TestingTask.DetectedFramework.PYTEST, TestingTask.detectFramework(tempDir))
  }

  @Test
  fun `detects vitest from package json`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("package.json"), """{"devDependencies":{"vitest":"^2.0.0"}}""")
    assertEquals(TestingTask.DetectedFramework.VITEST, TestingTask.detectFramework(tempDir))
  }

  @Test
  fun `detects go from go mod`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    assertEquals(TestingTask.DetectedFramework.GO, TestingTask.detectFramework(tempDir))
  }

  @Test
  fun `explicit command override is used for unit task`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(
      TestingContext(commands = TestingCommands(unit = "custom-test-command")),
      "test-unit",
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), listOf("--flag"))

    assertTrue(result.success)
    assertEquals("custom-test-command '--flag'", executor.command)
  }

  @Test
  fun `integration task is skipped when no gradle integration task exists`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { kotlin(\"jvm\") }\n")
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(TestingContext(), "test-integration")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertEquals(TaskResult.Status.SKIPPED, result.status)
    assertNull(executor.command)
  }

  @Test
  fun `coverage task parses jacoco report and enforces threshold`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("build.gradle.kts"), "tasks.register(\"jacocoTestReport\") {}\n")
    val reportDir = tempDir.resolve("build/reports/jacoco/test")
    Files.createDirectories(reportDir)
    Files.writeString(
      reportDir.resolve("jacocoTestReport.xml"),
      """
      <report name="sample">
        <counter type="LINE" missed="20" covered="80"/>
      </report>
      """.trimIndent(),
    )

    val task = registerAndGet(
      TestingContext(coverage = TestingCoverage(threshold = 85)),
      "test-coverage",
    )
    val executor = RecordingCommandExecutor(
      result = CommandResult(exitCode = 0, stdout = "coverage generated"),
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertFalse(result.success)
    assertTrue(result.message!!.contains("below threshold"))
    assertEquals(80.0, result.data["coveragePercent"])
  }

  @Test
  fun `coverage task parses lcov report from configured path`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    Files.writeString(
      tempDir.resolve("module-a.info"),
      """
      TN:
      SF:file.go
      LF:10
      LH:8
      end_of_record
      """.trimIndent(),
    )
    Files.writeString(
      tempDir.resolve("module-b.info"),
      """
      TN:
      SF:other.go
      LF:20
      LH:18
      end_of_record
      """.trimIndent(),
    )
    val task = registerAndGet(
      TestingContext(
        commands = TestingCommands(coverage = "echo 86%"),
        coverage = TestingCoverage(
          threshold = 80,
          reportPaths = listOf("module-a.info", "module-b.info"),
        ),
      ),
      "test-coverage",
    )
    val executor = RecordingCommandExecutor(
      result = CommandResult(exitCode = 0, stdout = "done"),
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("lcov", result.data["coverageSource"])
    assertEquals(86.66666666666667, result.data["coveragePercent"])
  }

  @Test
  fun `coverage task falls back to output percentage when reports are absent`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    val task = registerAndGet(TestingContext(coverage = TestingCoverage(threshold = 70)), "test-coverage")
    val executor = RecordingCommandExecutor(
      result = CommandResult(exitCode = 0, stdout = "coverage: 74.3% of statements"),
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals(74.3, result.data["coveragePercent"])
  }

  @Test
  fun `disabled plugin skips tasks`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    val task = registerAndGet(TestingContext(enabled = false), "test-unit")
    val executor = RecordingCommandExecutor()

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertEquals(TaskResult.Status.SKIPPED, result.status)
    assertNull(executor.command)
  }

  @Test
  fun `task rejects working directory traversal`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    val task = registerAndGet(TestingContext(workingDirectory = "../outside"), "test-unit")
    val executor = RecordingCommandExecutor()

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertFalse(result.success)
    assertNull(executor.command)
    assertTrue(result.message!!.contains("invalid working directory"))
  }

  @Test
  fun `plugin test kit can execute a detected unit task`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("go.mod"), "module example.com/testing\n")
    val kit = ArchitectPluginTestKit(TestingPlugin(), projectDir = tempDir)
      .configure(TestingContext())
      .withService(CommandExecutor::class.java, RecordingCommandExecutor())

    val result = kit.executeTask("test-unit")

    assertTrue(result.success)
    assertEquals("go-test", result.data["framework"])
  }

  private fun registerAndGet(ctx: TestingContext, taskId: String): Task {
    val plugin = TestingPlugin()
    plugin.init(ctx)
    val registry = TestTaskRegistry()
    plugin.register(registry)
    return registry.get(taskId)!!
  }

  private class TestTaskRegistry : TaskRegistry {
    private val tasks = mutableListOf<Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: Task) {
      tasks.add(task)
    }

    override fun get(id: String): Task? = tasks.find { it.id == id }

    override fun all(): List<Task> = tasks.toList()
  }

  private class RecordingCommandExecutor(
    private val result: CommandResult = CommandResult(exitCode = 0, stdout = "ok"),
  ) : CommandExecutor {
    var command: String? = null
    var workingDir: String? = null

    override fun execute(command: String, workingDir: String?) {
      this.command = command
      this.workingDir = workingDir
    }

    override fun executeWithResult(
      command: String,
      workingDir: String?,
      timeoutSeconds: Long,
      env: Map<String, String>,
    ): CommandResult {
      this.command = command
      this.workingDir = workingDir
      return result
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
