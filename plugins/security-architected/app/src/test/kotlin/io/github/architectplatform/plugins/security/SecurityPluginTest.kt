package io.github.architectplatform.plugins.security

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SecurityPluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = SecurityPlugin()
    assertEquals("security-plugin", plugin.id)
    assertEquals("security", plugin.contextKey)
    assertEquals(SecurityContext::class.java, plugin.ctxClass)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = SecurityPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    assertEquals(setOf("security-scan", "security-audit", "security-sbom"), registry.taskIds())
  }

  @Test
  fun `config schema is provided`() {
    val schema = SecurityPlugin().configSchema()
    assertNotNull(schema)
    assertEquals("object", schema["type"])
  }

  @Test
  fun `security scan uses trivy by default`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor(result = CommandResult(exitCode = 0, stdout = """{"Results":[]}"""))
    val task = registerAndGet(SecurityContext(), "security-scan")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("trivy fs --format json --quiet --severity CRITICAL --exit-code 0 .", executor.command)
    assertEquals("critical", result.data["threshold"])
  }

  @Test
  fun `security audit auto detects npm and fails on high vulnerabilities`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("package.json"), "{}")
    val executor = RecordingCommandExecutor(
      result = CommandResult(
        exitCode = 1,
        stdout = """
        {
          "metadata": {
            "vulnerabilities": {
              "info": 0,
              "low": 1,
              "moderate": 2,
              "high": 1,
              "critical": 0
            }
          }
        }
        """.trimIndent(),
      ),
    )
    val task = registerAndGet(SecurityContext(), "security-audit")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertFalse(result.success)
    assertEquals("npm audit --json", executor.command)
    assertEquals(4, result.data["issueCount"])
    assertEquals(mapOf("high" to 1, "medium" to 2, "low" to 1), result.data["severityCounts"])
  }

  @Test
  fun `security audit honors scan tool fallback for npm audit`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("package.json"), "{}")
    val executor = RecordingCommandExecutor(result = CommandResult(exitCode = 0, stdout = """{"metadata":{"vulnerabilities":{"low":1,"moderate":0,"high":0,"critical":0}}}"""))
    val context = SecurityContext(scan = SecurityScan(tools = listOf("trivy", "npm-audit")))
    val task = registerAndGet(context, "security-audit")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("npm audit --json", executor.command)
  }

  @Test
  fun `security sbom builds trivy command with configured output`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor(result = CommandResult(exitCode = 0, stdout = "generated"))
    val task = registerAndGet(
      SecurityContext(sbom = SecuritySbom(format = "spdx", output = "artifacts/sbom.json")),
      "security-sbom",
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals(
      "trivy fs --format spdx-json --output '${tempDir.resolve("artifacts/sbom.json")}' .",
      executor.command,
    )
    assertEquals(tempDir.resolve("artifacts/sbom.json").toString(), result.data["sbomOutput"])
  }

  @Test
  fun `disabled plugin skips tasks`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(SecurityContext(enabled = false), "security-scan")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertEquals(TaskResult.Status.SKIPPED, result.status)
    assertNull(executor.command)
  }

  @Test
  fun `task rejects working directory traversal`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(SecurityContext(workingDirectory = "../outside"), "security-scan")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertFalse(result.success)
    assertNull(executor.command)
    assertTrue(result.message!!.contains("invalid working directory"))
  }

  private fun registerAndGet(ctx: SecurityContext, taskId: String): Task {
    val plugin = SecurityPlugin()
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
    private val result: CommandResult = CommandResult(exitCode = 0, stdout = "{}"),
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
