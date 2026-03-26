package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PluginGraduationCheckerTest {

  @TempDir
  lateinit var tempDir: Path

  private fun compliantPlugin() = object : ArchitectPlugin<Map<String, Any>> {
    override val id = "test-plugin"
    override val contextKey = "test"
    override val ctxClass = Map::class.java as Class<Map<String, Any>>
    override var context: Map<String, Any> = emptyMap()
    override fun register(registry: TaskRegistry) {
      registry.add(io.github.architectplatform.api.core.tasks.builtin.SimpleTask(
        id = "test-task",
        description = "A test task",
        task = { _, _ -> io.github.architectplatform.api.core.tasks.TaskResult.success() },
      ))
    }
    override fun configSchema(): Map<String, Any> = mapOf("type" to "object")
  }

  @Test
  fun `compliant plugin passes all checks`() {
    Files.writeString(tempDir.resolve("README.md"), "# Test Plugin\n\nConfiguration example: `config: value`\n\nMore details about this plugin here for length requirement.")
    Files.writeString(tempDir.resolve("STATUS.md"), "# Status: Active")

    val result = PluginGraduationChecker.check(compliantPlugin(), tempDir)
    assertTrue(result.passed, "Expected all checks to pass but got failures: ${result.failures.map { it.name }}")
  }

  @Test
  fun `fails when README is missing`() {
    Files.writeString(tempDir.resolve("STATUS.md"), "# Status: Active")
    val result = PluginGraduationChecker.check(compliantPlugin(), tempDir)
    assertFalse(result.passed)
    assertTrue(result.failures.any { it.name == "readme" })
  }

  @Test
  fun `fails when STATUS is missing`() {
    Files.writeString(tempDir.resolve("README.md"), "# Test Plugin\n\nConfiguration example: `config: value`\n\nMore details about this plugin here for length requirement.")
    val result = PluginGraduationChecker.check(compliantPlugin(), tempDir)
    assertFalse(result.passed)
    assertTrue(result.failures.any { it.name == "status-file" })
  }

  @Test
  fun `warns when configSchema is null`() {
    Files.writeString(tempDir.resolve("README.md"), "# Test Plugin\n\nConfiguration example: `config: value`\n\nMore details about this plugin here for length requirement.")
    Files.writeString(tempDir.resolve("STATUS.md"), "# Status: Active")

    val plugin = object : ArchitectPlugin<Map<String, Any>> {
      override val id = "no-schema-plugin"
      override val contextKey = "noschema"
      override val ctxClass = Map::class.java as Class<Map<String, Any>>
      override var context: Map<String, Any> = emptyMap()
      override fun register(registry: TaskRegistry) {
        registry.add(io.github.architectplatform.api.core.tasks.builtin.SimpleTask(
          id = "task", description = "task",
          task = { _, _ -> io.github.architectplatform.api.core.tasks.TaskResult.success() },
        ))
      }
    }
    val result = PluginGraduationChecker.check(plugin, tempDir)
    // config-schema is WARNING severity, so it doesn't fail the overall check
    assertTrue(result.warnings.any { it.name == "config-schema" })
  }

  @Test
  fun `fails for blank plugin id`() {
    val plugin = object : ArchitectPlugin<Map<String, Any>> {
      override val id = ""
      override val contextKey = "test"
      override val ctxClass = Map::class.java as Class<Map<String, Any>>
      override var context: Map<String, Any> = emptyMap()
      override fun register(registry: TaskRegistry) {}
    }
    val result = PluginGraduationChecker.check(plugin, tempDir)
    assertFalse(result.passed)
    assertTrue(result.failures.any { it.name == "plugin-id" })
  }
}
