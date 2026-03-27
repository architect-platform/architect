package io.github.architectplatform.cli.plugin

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.project.ProjectContext
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.outputStream
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PluginJarTesterTest {
  private val validator = PluginJarValidator()

  @Test
  fun `plugin test passes for valid plugin with schema and task`(@TempDir tempDir: Path) {
    val jarPath = createPluginJar(
      tempDir.resolve("valid-plugin-test.jar"),
      TestPluginWithSchema::class.java,
      TestPluginContext::class.java,
      TestPluginTask::class.java,
    )

    val result = validator.test(jarPath)

    assertTrue(result.valid, "Expected valid result but got errors=${result.errors}, warnings=${result.warnings}, checks=${result.plugins.firstOrNull()?.checks}")
    val plugin = result.plugins.first { it.pluginId == "test-plugin-with-schema" }
    assertTrue(plugin.checks.any { it.name == "contract" && it.passed })
    assertTrue(plugin.checks.any { it.name == "config-schema" && it.passed })
    assertTrue(plugin.checks.any { it.name == "task-registration" && it.passed })
  }

  @Test
  fun `plugin test fails when schema is missing`(@TempDir tempDir: Path) {
    val jarPath = createPluginJar(
      tempDir.resolve("missing-schema-plugin-test.jar"),
      TestPluginMissingSchema::class.java,
      TestPluginContext::class.java,
      TestPluginTask::class.java,
    )

    val result = validator.test(jarPath)

    assertFalse(result.valid)
    val plugin = result.plugins.first { it.pluginId == "test-plugin-missing-schema" }
    assertTrue(plugin.checks.any { it.name == "config-schema" && !it.passed })
  }

  private fun createPluginJar(jarPath: Path, pluginClass: Class<*>, vararg extraClasses: Class<*>): Path {
    JarOutputStream(jarPath.outputStream().buffered()).use { output ->
      writeClass(output, pluginClass)
      extraClasses.forEach { writeClass(output, it) }
      writeTextEntry(output, PluginJarValidator.SPI_RESOURCE, pluginClass.name + "\n")
    }
    return jarPath
  }

  private fun writeClass(output: JarOutputStream, type: Class<*>) {
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
}

data class TestPluginContext(
  val enabled: Boolean = true,
)

class TestPluginWithSchema : ArchitectPlugin<TestPluginContext> {
  override val id: String = "test-plugin-with-schema"
  override val contextKey: String = "testPluginWithSchema"
  override val ctxClass: Class<TestPluginContext> = TestPluginContext::class.java
  override var context: TestPluginContext = TestPluginContext()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean"),
    ),
  )

  override fun register(registry: TaskRegistry) {
    registry.add(TestPluginTask())
  }
}

class TestPluginMissingSchema : ArchitectPlugin<TestPluginContext> {
  override val id: String = "test-plugin-missing-schema"
  override val contextKey: String = "testPluginMissingSchema"
  override val ctxClass: Class<TestPluginContext> = TestPluginContext::class.java
  override var context: TestPluginContext = TestPluginContext()

  override fun register(registry: TaskRegistry) {
    registry.add(TestPluginTask())
  }
}

class TestPluginTask : Task {
  override val id: String = "plugin-test-task"
  override fun description(): String = "Task for plugin test command validation"
  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult = TaskResult.success()
}
