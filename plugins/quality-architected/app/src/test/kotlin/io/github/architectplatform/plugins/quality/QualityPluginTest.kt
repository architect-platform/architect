package io.github.architectplatform.plugins.quality

import io.github.architectplatform.api.components.execution.CommandExecutor
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

class QualityPluginTest {

  @Test
  fun `plugin id and context key`() {
    val plugin = QualityPlugin()
    assertEquals("quality-plugin", plugin.id)
    assertEquals("quality", plugin.contextKey)
    assertEquals(QualityContext::class.java, plugin.ctxClass)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = QualityPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("quality-lint" in ids)
    assertTrue("quality-analyze" in ids)
    assertTrue("quality-report" in ids)
    assertTrue("quality-gate" in ids)
    assertEquals(4, ids.size)
  }

  @Test
  fun `init applies context`() {
    val plugin = QualityPlugin()
    val ctx = QualityContext(enabled = false)
    plugin.init(ctx)
    assertFalse(plugin.context.enabled)
  }

  @Test
  fun `config schema is provided`() {
    val plugin = QualityPlugin()
    val schema = plugin.configSchema()
    assertNotNull(schema)
    assertEquals("object", schema["type"])
    @Suppress("UNCHECKED_CAST")
    val props = schema["properties"] as Map<String, Any>
    assertTrue("enabled" in props)
    assertTrue("tools" in props)
    assertTrue("gates" in props)
  }

  // -- Disabled context tests --

  @Test
  fun `disabled context skips lint task`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(enabled = false), "quality-lint")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.message?.contains("disabled") == true)
    assertNull(executor.command)
  }

  @Test
  fun `disabled context skips analyze task`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(enabled = false), "quality-analyze")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.message?.contains("disabled") == true)
    assertNull(executor.command)
  }

  @Test
  fun `disabled context skips report task`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(enabled = false), "quality-report")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.message?.contains("disabled") == true)
    assertNull(executor.command)
  }

  @Test
  fun `disabled context skips gate task`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(enabled = false), "quality-gate")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.message?.contains("disabled") == true)
    assertNull(executor.command)
  }

  // -- Auto-detection tests --

  @Test
  fun `lint detects detekt when build gradle kts and detekt yml present`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("build.gradle.kts"))
    Files.createFile(tempDir.resolve("detekt.yml"))
    val linter = QualityTask.detectLinter(tempDir.toString())
    assertEquals("./gradlew detekt", linter)
  }

  @Test
  fun `lint detects eslint when package json and eslintrc present`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("package.json"))
    Files.createFile(tempDir.resolve(".eslintrc.json"))
    val linter = QualityTask.detectLinter(tempDir.toString())
    assertEquals("npx eslint .", linter)
  }

  @Test
  fun `lint detects eslint with flat config`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("package.json"))
    Files.createFile(tempDir.resolve("eslint.config.js"))
    val linter = QualityTask.detectLinter(tempDir.toString())
    assertEquals("npx eslint .", linter)
  }

  @Test
  fun `lint detects ruff when pyproject toml with tool ruff present`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("pyproject.toml"))
    Files.writeString(tempDir.resolve("pyproject.toml"), "[tool.ruff]\nline-length = 120\n")
    val linter = QualityTask.detectLinter(tempDir.toString())
    assertEquals("ruff check .", linter)
  }

  @Test
  fun `lint detects clippy when Cargo toml present`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("Cargo.toml"))
    val linter = QualityTask.detectLinter(tempDir.toString())
    assertEquals("cargo clippy", linter)
  }

  @Test
  fun `lint returns null when no tool detected`(@TempDir tempDir: Path) {
    val linter = QualityTask.detectLinter(tempDir.toString())
    assertNull(linter)
  }

  // -- Lint execution tests --

  @Test
  fun `lint executes detected linter command`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("Cargo.toml"))
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(), "quality-lint")
    val result = task.execute(TestEnvironment(executor), projectContext(tempDir), emptyList())
    assertTrue(result.success)
    assertEquals("cargo clippy", executor.command)
  }

  @Test
  fun `lint falls back to echo when no linter detected`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(), "quality-lint")
    val result = task.execute(TestEnvironment(executor), projectContext(tempDir), emptyList())
    assertTrue(result.success)
    assertTrue(executor.command?.contains("No linter detected") == true)
  }

  @Test
  fun `lint returns failure on executor exception`(@TempDir tempDir: Path) {
    Files.createFile(tempDir.resolve("Cargo.toml"))
    val task = registerAndGet(QualityContext(), "quality-lint")
    val result = task.execute(TestEnvironment(FailingCommandExecutor()), projectContext(tempDir), emptyList())
    assertFalse(result.success)
    assertTrue(result.message?.contains("failed") == true)
  }

  // -- Analyze tests --

  @Test
  fun `analyze uses sonarqube when configured`() {
    val ctx = QualityContext(
      tools = listOf(QualityTool(name = "sonarqube", url = "https://sonar.example.com", projectKey = "my-project")),
    )
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(ctx, "quality-analyze")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.success)
    assertNotNull(executor.command)
    assertTrue(executor.command!!.contains("sonar-scanner"))
    assertTrue(executor.command!!.contains("sonar.host.url"))
    assertTrue(executor.command!!.contains("sonar.projectKey"))
  }

  @Test
  fun `analyze uses codeclimate when configured`() {
    val ctx = QualityContext(tools = listOf(QualityTool(name = "codeclimate")))
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(ctx, "quality-analyze")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.success)
    assertEquals("codeclimate analyze", executor.command)
  }

  @Test
  fun `analyze falls back when no tool configured`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(), "quality-analyze")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.success)
    assertTrue(executor.command?.contains("No static analysis tool configured") == true)
  }

  // -- Report tests --

  @Test
  fun `report generates summary with gates`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(), "quality-report")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.success)
    assertNotNull(executor.command)
    assertTrue(executor.command!!.contains("Quality Report"))
    assertTrue(executor.command!!.contains("coverage>=80%"))
  }

  // -- Gate tests --

  @Test
  fun `gate passes with valid default gates`() {
    val (passed, message) = QualityTask.checkGates(QualityGates())
    assertTrue(passed)
    assertTrue(message.contains("coverage>=80%"))
  }

  @Test
  fun `gate passes with custom gates`() {
    val (passed, message) = QualityTask.checkGates(QualityGates(coverage = 90, duplications = 5, bugs = 1, vulnerabilities = 0))
    assertTrue(passed)
    assertTrue(message.contains("coverage>=90%"))
  }

  @Test
  fun `gate fails with invalid coverage`() {
    val (passed, message) = QualityTask.checkGates(QualityGates(coverage = -1))
    assertFalse(passed)
    assertTrue(message.contains("Invalid coverage"))
  }

  @Test
  fun `gate fails with invalid bugs`() {
    val (passed, message) = QualityTask.checkGates(QualityGates(bugs = -5))
    assertFalse(passed)
    assertTrue(message.contains("Invalid bugs"))
  }

  @Test
  fun `gate task executes with valid gates`() {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(QualityContext(), "quality-gate")
    val result = task.execute(TestEnvironment(executor), projectContext(), emptyList())
    assertTrue(result.success)
    assertNotNull(executor.command)
    assertTrue(executor.command!!.contains("Quality gate check"))
  }

  // -- Helper methods and classes --

  private fun registerAndGet(ctx: QualityContext, taskId: String): Task {
    val plugin = QualityPlugin()
    plugin.init(ctx)
    val registry = TestTaskRegistry()
    plugin.register(registry)
    return registry.get(taskId)!!
  }

  private fun projectContext(dir: Path = Path.of("/repo")) = ProjectContext(dir, emptyMap())

  private class TestTaskRegistry : TaskRegistry {
    private val tasks = mutableListOf<Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: Task) { tasks.add(task) }
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
