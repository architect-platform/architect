package io.github.architectplatform.core.plugin.infra

import io.github.architectplatform.core.common.Result
import io.github.architectplatform.core.plugin.domain.PluginSourceConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class LocalPluginSourceTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should resolve local plugin within base directory`() {
    val pluginFile = tempDir.resolve("plugins/test.jar")
    Files.createDirectories(pluginFile.parent)
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