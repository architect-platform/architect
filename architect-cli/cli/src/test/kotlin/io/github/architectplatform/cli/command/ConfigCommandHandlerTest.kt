package io.github.architectplatform.cli.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class ConfigCommandHandlerTest {

  @Test
  fun `config lint passes for valid config and accepted plugin variants`(@TempDir dir: Path) {
    File(dir.toFile(), "architect.yml").writeText(
      """
      project:
        name: sample
      plugins:
        - git context
        - docs-plugin
      docs:
        siteName: Sample Docs
      """.trimIndent(),
    )

    var exitCode: Int? = null
    val output = withUserDir(dir) {
      captureStdout {
        ConfigCommandHandler(exit = { code -> exitCode = code }).handle(listOf("config", "lint"))
      }
    }

    assertNull(exitCode)
    assertTrue(output.contains("Configuration lint passed"))
  }

  @Test
  fun `config lint fails on invalid values and exits with code 1`(@TempDir dir: Path) {
    File(dir.toFile(), "architect.yml").writeText(
      """
      project:
        description: missing name
      plugins: git-architected
      """.trimIndent(),
    )

    var exitCode: Int? = null
    val output = withUserDir(dir) {
      captureStdout {
        ConfigCommandHandler(exit = { code -> exitCode = code }).handle(listOf("config", "lint"))
      }
    }

    assertEquals(1, exitCode)
    assertTrue(output.contains("Missing 'project.name'"))
    assertTrue(output.contains("'plugins' should be a list"))
  }

  @Test
  fun `config lint reports deprecated keys unknown plugin and suggestions`(@TempDir dir: Path) {
    File(dir.toFile(), "architect.yml").writeText(
      """
      proejct:
        name: typo
      docs:
        site_name: Legacy Docs
      scripts:
        build: ./gradlew build
      plugins:
        - name: gti-architected
      """.trimIndent(),
    )

    var exitCode: Int? = null
    val output = withUserDir(dir) {
      captureStdout {
        ConfigCommandHandler(exit = { code -> exitCode = code }).handle(listOf("config", "lint"))
      }
    }

    assertEquals(1, exitCode)
    assertTrue(output.contains("Deprecated top-level key 'scripts'"))
    assertTrue(output.contains("Deprecated key 'docs.site_name'"))
    assertTrue(output.contains("Unknown top-level key 'proejct'. Did you mean 'project'?"))
    assertTrue(output.contains("Unknown plugin 'gti-architected'. Did you mean 'git-architected'?"))
  }

  private fun captureStdout(block: () -> Unit): String {
    val original = System.out
    val output = ByteArrayOutputStream()
    System.setOut(PrintStream(output))
    try {
      block()
    } finally {
      System.setOut(original)
    }
    return output.toString()
  }

  private fun <T> withUserDir(dir: Path, block: () -> T): T {
    val original = System.getProperty("user.dir")
    System.setProperty("user.dir", dir.toString())
    return try {
      block()
    } finally {
      System.setProperty("user.dir", original)
    }
  }
}
