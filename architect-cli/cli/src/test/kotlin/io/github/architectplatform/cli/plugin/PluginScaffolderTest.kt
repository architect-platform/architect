package io.github.architectplatform.cli.plugin

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PluginScaffolderTest {
  private val scaffolder = PluginScaffolder()

  @Test
  fun `scaffolds kotlin plugin template`(@TempDir tempDir: Path) {
    val pluginDir = scaffolder.scaffold("hello-world", PluginTemplate.KOTLIN, tempDir)

    assertTrue(pluginDir.resolve("plugin.yml").exists())
    assertTrue(pluginDir.resolve("README.md").exists())
    assertTrue(pluginDir.resolve("app/build.gradle.kts").exists())
    assertTrue(
      pluginDir.resolve(
        "app/src/main/kotlin/io/github/architectplatform/plugins/helloworld/HelloWorldPlugin.kt"
      ).exists()
    )
    assertTrue(
      pluginDir.resolve(
        "app/src/test/kotlin/io/github/architectplatform/plugins/helloworld/HelloWorldPluginTest.kt"
      ).exists()
    )
    assertTrue(pluginDir.resolve("plugin.yml").readText().contains("template: kotlin"))
  }

  @Test
  fun `scaffolds typescript plugin template`(@TempDir tempDir: Path) {
    val pluginDir = scaffolder.scaffold("frontend-tools", PluginTemplate.TYPESCRIPT, tempDir)

    assertTrue(pluginDir.resolve("plugin.yml").exists())
    assertTrue(pluginDir.resolve("package.json").exists())
    assertTrue(pluginDir.resolve("tsconfig.json").exists())
    assertTrue(pluginDir.resolve("src/plugin.ts").exists())
    assertTrue(pluginDir.resolve("test/plugin.test.ts").exists())
    assertTrue(pluginDir.resolve("package.json").readText().contains("@architect-platform/plugin-sdk"))
  }

  @Test
  fun `scaffolds go plugin template`(@TempDir tempDir: Path) {
    val pluginDir = scaffolder.scaffold("release-tools", PluginTemplate.GO, tempDir)

    assertTrue(pluginDir.resolve("plugin.yml").exists())
    assertTrue(pluginDir.resolve("go.mod").exists())
    assertTrue(pluginDir.resolve("plugin.go").exists())
    assertTrue(pluginDir.resolve("main.go").exists())
    assertTrue(pluginDir.resolve("plugin_test.go").exists())
    assertTrue(pluginDir.resolve("go.mod").readText().contains("plugin-sdk-go"))
  }

  @Test
  fun `normalizes plugin directory names`(@TempDir tempDir: Path) {
    val pluginDir = scaffolder.scaffold(" Fancy Plugin ", PluginTemplate.KOTLIN, tempDir)

    assertTrue(Files.isDirectory(pluginDir))
    assertTrue(pluginDir.fileName.toString() == "fancy-plugin")
  }
}
