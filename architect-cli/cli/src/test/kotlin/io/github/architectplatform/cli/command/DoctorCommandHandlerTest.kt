package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.engine.EngineHealthChecker
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class DoctorCommandHandlerTest {

  @Test
  fun `handle prints detected project profile`(@TempDir dir: Path) {
    File(dir.toFile(), "package.json").writeText(
      """
        {
          "devDependencies": {
            "typescript": "^5.0.0",
            "vitest": "^2.0.0"
          }
        }
      """.trimIndent()
    )
    File(dir.toFile(), "tsconfig.json").writeText("{}")
    dir.resolve(".github/workflows").toFile().mkdirs()

    val output = captureStdout {
      withUserDir(dir) {
        DoctorCommandHandler(FakeEngineHealthChecker()).apply {
          plain = true
        }.handle(emptyList())
      }
    }

    assertTrue(output.contains("Detected project profile:"))
    assertTrue(output.contains("Languages: TypeScript"))
    assertTrue(output.contains("Build tools: npm"))
    assertTrue(output.contains("Test frameworks: Vitest"))
    assertTrue(output.contains("CI systems: GitHub Actions"))
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

  private fun withUserDir(dir: Path, block: () -> Unit) {
    val original = System.getProperty("user.dir")
    System.setProperty("user.dir", dir.toString())
    try {
      block()
    } finally {
      System.setProperty("user.dir", original)
    }
  }

  private class FakeEngineHealthChecker : EngineHealthChecker() {
    override fun isRunning(): Boolean = false
  }
}
