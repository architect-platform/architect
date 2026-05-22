package io.github.architectplatform.cli.integration

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask
import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher
import io.github.architectplatform.cli.history.LocalHistoryReader
import java.io.File
import java.net.URLDecoder
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class EmbeddedModeLocalPluginIntegrationTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should load local plugin jar execute task and persist history`() {
    val originalHome = System.getProperty("user.home")
    System.setProperty("user.home", tempDir.toString())
    try {
      val projectName = "embedded-local-plugin-project"
      val projectDir = tempDir.resolve(projectName)
      val pluginsDir = projectDir.resolve("plugins")
      pluginsDir.createDirectories()
      val pluginJar = createPluginJar(pluginsDir.resolve("embedded-local-plugin.jar"))

      File(projectDir.toFile(), "architect.yml").writeText(
        """
        project:
          name: $projectName
        plugins:
          - name: embedded-local-plugin
            type: local
            path: plugins/${pluginJar.fileName}
        """.trimIndent() + "\n"
      )

      val executor = EmbeddedTaskExecutor(JdkRemoteContentFetcher())
      val observedEventIds = mutableListOf<String>()

      val result = executor.execute(projectName, projectDir.toString(), "local-hello", emptyList()) {
        observedEventIds += it.id
      }

      val history = LocalHistoryReader().getByProject(projectName, limit = 5)

      assertTrue(result.success, result.message ?: "Expected embedded local plugin task to succeed")
      assertTrue(observedEventIds.contains("task.started"))
      assertTrue(observedEventIds.contains("task.completed"))
      assertTrue(history.isNotEmpty())
      assertEquals(projectName, history.first().project)
      assertEquals("local-hello", history.first().task)
      assertTrue(history.first().success)
    } finally {
      System.setProperty("user.home", originalHome)
    }
  }

  private fun createPluginJar(jarPath: Path): Path {
    JarOutputStream(jarPath.outputStream().buffered()).use { output ->
      writeClassFamily(output, EmbeddedLocalPlugin::class.java)
      writeClass(output, EmbeddedLocalPluginContext::class.java)
      writeTextEntry(
        output,
        "META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin",
        EmbeddedLocalPlugin::class.java.name + "\n",
      )
    }
    return jarPath
  }

  private fun writeClass(output: JarOutputStream, type: Class<*>) {
    val resourcePath = type.name.replace('.', '/') + ".class"
    val bytes = type.classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes() }
      ?: error("Missing compiled class resource $resourcePath")
    output.putNextEntry(JarEntry(resourcePath))
    output.write(bytes)
    output.closeEntry()
  }

  private fun writeClassFamily(output: JarOutputStream, type: Class<*>) {
    writeClass(output, type)

    val resourcePath = type.name.replace('.', '/') + ".class"
    val resourceUrl = type.classLoader.getResource(resourcePath) ?: return
    if (resourceUrl.protocol != "file") {
      return
    }

    val classFile = File(URLDecoder.decode(resourceUrl.path, Charsets.UTF_8))
    val directory = classFile.parentFile ?: return
    val prefix = type.simpleName + "$"
    val packagePath = type.packageName.replace('.', '/')

    directory.listFiles { file -> file.name.startsWith(prefix) && file.name.endsWith(".class") }
      ?.sortedBy { it.name }
      ?.forEach { nestedClassFile ->
        val entryName = "$packagePath/${nestedClassFile.name}"
        output.putNextEntry(JarEntry(entryName))
        output.write(nestedClassFile.readBytes())
        output.closeEntry()
      }
  }

  private fun writeTextEntry(output: JarOutputStream, entryName: String, content: String) {
    output.putNextEntry(JarEntry(entryName))
    output.write(content.toByteArray())
    output.closeEntry()
  }
}

data class EmbeddedLocalPluginContext(
  val enabled: Boolean = true,
)

class EmbeddedLocalPlugin : ArchitectPlugin<EmbeddedLocalPluginContext> {
  override val id: String = "embedded-local-plugin"
  override val contextKey: String = "embeddedLocal"
  override val ctxClass: Class<EmbeddedLocalPluginContext> = EmbeddedLocalPluginContext::class.java
  override var context: EmbeddedLocalPluginContext = EmbeddedLocalPluginContext()

  override fun register(registry: TaskRegistry) {
    registry.add(
      SimpleTask(
        id = "local-hello",
        description = "Hello from a local plugin jar",
        task = { _, _ -> TaskResult.success("Local plugin task completed") },
      )
    )
  }
}
