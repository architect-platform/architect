package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.core.common.Result
import io.github.architectplatform.core.plugin.domain.PluginSourceConfig
import io.github.architectplatform.core.plugin.infra.LocalPluginSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LocalPluginSourceTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should resolve local plugin within base directory`() {
    val pluginDir = tempDir.resolve("plugins")
    Files.createDirectories(pluginDir)
    val pluginFile = pluginDir.resolve("test.jar")
    pluginFile.writeText("stub")

    val result = LocalPluginSource().resolve(
      PluginSourceConfig(
        type = "local",
        name = "test-plugin",
        version = "1.0.0",
        path = "plugins/test.jar",
        baseDir = tempDir.toString(),
      ),
    )

    assertTrue(result.isSuccess())
    assertEquals(pluginFile.toFile().absolutePath, result.getOrThrow().absolutePath)
  }

  @Test
  fun `should reject local plugin path traversal`() {
    val result = LocalPluginSource().resolve(
      PluginSourceConfig(
        type = "local",
        name = "test-plugin",
        version = "1.0.0",
        path = "../escape.jar",
        baseDir = tempDir.toString(),
      ),
    )

    assertTrue(result.isFailure())
    assertTrue((result as Result.Failure).message.contains("project root"))
  }
}