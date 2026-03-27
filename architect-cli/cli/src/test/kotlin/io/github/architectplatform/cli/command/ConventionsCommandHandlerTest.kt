package io.github.architectplatform.cli.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path
import kotlin.io.path.writeText

class ConventionsCommandHandlerTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `handle applies kotlin conventions preset and architecture plugin`() {
    tempDir.resolve("architect.yml").writeText(
      """
      project:
        name: sample
      plugins:
        - name: git-architected
          repo: architect-platform/architect
      """.trimIndent()
    )

    val originalUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", tempDir.toString())
    try {
      ConventionsCommandHandler().handle(listOf("conventions", "kotlin"))
    } finally {
      System.setProperty("user.dir", originalUserDir)
    }

    @Suppress("UNCHECKED_CAST")
    val config = Yaml().load<Map<String, Any>>(tempDir.resolve("architect.yml").toFile().inputStream())
    val plugins = config["plugins"] as List<Map<String, Any>>
    assertTrue(plugins.any { it["name"] == "architecture-architected" })

    val architecture = config["architecture"] as Map<String, Any>
    assertEquals(listOf("kotlin-conventions"), architecture["presetRulesets"])
  }
}
