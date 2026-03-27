package io.github.architectplatform.cli.plugin

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.outputStream
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PluginJarValidatorTest {
  private val validator = PluginJarValidator()

  @Test
  fun `validates plugin jar with spi implementation and config deserialization`(@TempDir tempDir: Path) {
    val jarPath = createPluginJar(tempDir.resolve("valid-plugin.jar"), ValidPlugin::class.java, ValidContext::class.java)

    val result = validator.validate(jarPath)

    assertTrue(result.valid)
    assertTrue(result.errors.isEmpty())
    assertTrue(result.plugins.any { it.pluginId == "valid-plugin" && it.configDeserializationValid })
  }

  @Test
  fun `fails validation when spi descriptor is missing`(@TempDir tempDir: Path) {
    val jarPath = tempDir.resolve("missing-spi.jar")
    JarOutputStream(jarPath.outputStream().buffered()).use { output ->
      writeClass(output, ValidPlugin::class.java)
      writeClass(output, ValidContext::class.java)
    }

    val result = validator.validate(jarPath)

    assertFalse(result.valid)
    assertTrue(result.errors.any { it.contains("Missing SPI descriptor") })
  }

  @Test
  fun `fails validation when plugin context cannot deserialize from config`(@TempDir tempDir: Path) {
    val jarPath = createPluginJar(tempDir.resolve("invalid-config.jar"), InvalidConfigPlugin::class.java, InvalidConfigContext::class.java)

    val result = validator.validate(jarPath)

    assertFalse(result.valid)
    assertTrue(result.errors.any { it.contains("failed config deserialization") })
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

data class ValidContext(
  val enabled: Boolean = true,
)

class ValidPlugin : ArchitectPlugin<ValidContext> {
  override val id: String = "valid-plugin"
  override val contextKey: String = "valid"
  override val ctxClass: Class<ValidContext> = ValidContext::class.java
  override var context: ValidContext = ValidContext()

  override fun register(registry: TaskRegistry) = Unit
}

data class InvalidConfigContext(
  val token: String,
)

class InvalidConfigPlugin : ArchitectPlugin<InvalidConfigContext> {
  override val id: String = "invalid-config-plugin"
  override val contextKey: String = "invalid"
  override val ctxClass: Class<InvalidConfigContext> = InvalidConfigContext::class.java
  override lateinit var context: InvalidConfigContext

  override fun register(registry: TaskRegistry) = Unit
}
