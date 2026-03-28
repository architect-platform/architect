package io.github.architectplatform.cli.command

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CheckCommandHandlerTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `check fails on missing vars discovered from plugin metadata schema and config and updates env example`() {
    val projectDir = tempDir.resolve("env-check-project")
    val pluginsDir = projectDir.resolve("plugins")
    pluginsDir.createDirectories()
    createPluginJar(pluginsDir.resolve("required-env-plugin.jar"))

    projectDir.resolve("architect.yml").toFile().writeText(
      """
      project:
        name: env-check-project
      plugins:
        - name: required-env-plugin
          type: local
          path: plugins/required-env-plugin.jar
      tasks:
        env-task:
          description: ${'$'}{env.OPTIONAL_TOKEN:https://example.test}
          run: echo ${'$'}{env.APP_ENDPOINT}
      """.trimIndent() + "\n",
    )
    projectDir.resolve(".env.example").toFile().writeText("EXISTING_KEY=\n")

    val exitCodes = mutableListOf<Int>()
    val handler = CheckCommandHandler(
      embeddedTaskExecutor = EmbeddedTaskExecutor(JdkRemoteContentFetcher()),
      extractProjectName = { it.substringAfterLast("/") },
      exit = exitCodes::add,
      envProvider = { emptyMap() },
    )

    val output = setUserDir(projectDir) {
      captureStdout { handler.handle(listOf("check")) }
    }

    assertEquals(listOf(1), exitCodes)
    assertTrue(output.contains("Required environment variable 'APP_ENDPOINT' is not set"))
    assertTrue(output.contains("Required environment variable 'PLUGIN_METADATA_TOKEN' is not set"))
    assertTrue(output.contains("Required environment variable 'SCHEMA_LEVEL_TOKEN' is not set"))
    assertTrue(output.contains("Required environment variable 'NESTED_SCHEMA_TOKEN' is not set"))
    assertEquals(
      listOf(
        "EXISTING_KEY=",
        "APP_ENDPOINT=",
        "NESTED_SCHEMA_TOKEN=",
        "OPTIONAL_TOKEN=",
        "PLUGIN_METADATA_TOKEN=",
        "SCHEMA_LEVEL_TOKEN=",
      ),
      Files.readAllLines(projectDir.resolve(".env.example")),
    )
  }

  @Test
  fun `check honors profile specific dotenv files`() {
    val projectDir = tempDir.resolve("profile-aware-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").toFile().writeText(
      """
      project:
        name: profile-aware-project
      tasks:
        profile-task:
          run: echo ${'$'}{env.CI_ONLY_TOKEN}
      """.trimIndent() + "\n",
    )
    projectDir.resolve(".env.ci").toFile().writeText("CI_ONLY_TOKEN=available-in-ci\n")

    val defaultExitCodes = mutableListOf<Int>()
    val defaultHandler = CheckCommandHandler(
      embeddedTaskExecutor = EmbeddedTaskExecutor(JdkRemoteContentFetcher()),
      extractProjectName = { it.substringAfterLast("/") },
      exit = defaultExitCodes::add,
      envProvider = { emptyMap() },
    )

    val defaultOutput = setUserDir(projectDir) {
      captureStdout { defaultHandler.handle(listOf("check")) }
    }

    val ciExitCodes = mutableListOf<Int>()
    val ciExecutor = EmbeddedTaskExecutor(JdkRemoteContentFetcher()).also { it.activeProfile = "ci" }
    val ciHandler = CheckCommandHandler(
      embeddedTaskExecutor = ciExecutor,
      extractProjectName = { it.substringAfterLast("/") },
      exit = ciExitCodes::add,
      envProvider = { emptyMap() },
    )

    val ciOutput = setUserDir(projectDir) {
      captureStdout { ciHandler.handle(listOf("check")) }
    }

    assertEquals(listOf(1), defaultExitCodes)
    assertTrue(defaultOutput.contains("CI_ONLY_TOKEN"))
    assertTrue(ciExitCodes.isEmpty())
    assertTrue(ciOutput.contains("1 ready, 0 blocked"))
  }

  private fun createPluginJar(jarPath: Path): Path {
    JarOutputStream(jarPath.outputStream().buffered()).use { output ->
      writeClassFamily(output, RequiredEnvPlugin::class.java)
      writeClass(output, RequiredEnvPluginContext::class.java)
      writeTextEntry(
        output,
        "META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin",
        RequiredEnvPlugin::class.java.name + "\n",
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

  private fun captureStdout(block: () -> Unit): String {
    val baos = ByteArrayOutputStream()
    val originalOut = System.out
    System.setOut(PrintStream(baos))
    try {
      block()
    } finally {
      System.setOut(originalOut)
    }
    return baos.toString()
  }

  private fun <T> setUserDir(projectDir: Path, block: () -> T): T {
    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", projectDir.toString())
    return try {
      block()
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }
  }
}

data class RequiredEnvPluginContext(
  val enabled: Boolean = true,
)

class RequiredEnvPlugin : ArchitectPlugin<RequiredEnvPluginContext> {
  override val id: String = "required-env-plugin"
  override val contextKey: String = "requiredEnv"
  override val ctxClass: Class<RequiredEnvPluginContext> = RequiredEnvPluginContext::class.java
  override var context: RequiredEnvPluginContext = RequiredEnvPluginContext()

  override fun register(registry: TaskRegistry) = Unit

  override fun requiredEnvironmentVariables(): Set<String> =
    if (context.enabled) setOf("PLUGIN_METADATA_TOKEN") else emptySet()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "requiredEnv" to listOf("SCHEMA_LEVEL_TOKEN"),
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean", "default" to true),
      "service" to mapOf(
        "type" to "object",
        "x-required-env" to listOf("NESTED_SCHEMA_TOKEN"),
        "properties" to mapOf(
          "baseUrl" to mapOf("type" to "string"),
        ),
      ),
    ),
  )
}
