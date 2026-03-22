package io.github.architectplatform.cli.plugin

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PluginDocumentationGeneratorTest {
  private val scaffolder = PluginScaffolder()
  private val generator = PluginDocumentationGenerator()

  @Test
  fun `generates markdown reference for scaffolded kotlin plugin`(@TempDir tempDir: Path) {
    val pluginDir = scaffolder.scaffold("hello-world", PluginTemplate.KOTLIN, tempDir)

    val documentation = generator.generate(pluginDir)

    assertTrue(documentation.outputPath.exists())
    val markdown = documentation.outputPath.readText()
    assertTrue(markdown.contains("# hello-world Plugin Reference"))
    assertTrue(markdown.contains("| Template | `kotlin` |"))
    assertTrue(markdown.contains("### `hello-world-hello`"))
    assertTrue(markdown.contains("- Phase: `BUILD`"))
    assertTrue(markdown.contains("./gradlew build"))
    assertEquals("hello-world", documentation.manifest.name)
  }

  @Test
  fun `generates markdown reference from plugin manifest path`(@TempDir tempDir: Path) {
    val pluginDir = scaffolder.scaffold("frontend-tools", PluginTemplate.TYPESCRIPT, tempDir)

    val documentation = generator.generate(pluginDir.resolve("plugin.yml"))

    assertTrue(documentation.outputPath.exists())
    val markdown = documentation.outputPath.readText()
    assertTrue(markdown.contains("# frontend-tools Plugin Reference"))
    assertTrue(markdown.contains("| Entrypoint | `node dist/src/index.js` |"))
    assertTrue(markdown.contains("### `frontend-tools-hello`"))
    assertTrue(markdown.contains("npm run build"))
  }
}