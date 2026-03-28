package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.dto.MonorepoHealthDTO
import io.github.architectplatform.cli.dto.ProjectHealthDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class OutputFormatterHealthTest {

  private fun captureOutput(block: () -> Unit): String {
    val baos = ByteArrayOutputStream()
    val original = System.out
    System.setOut(PrintStream(baos))
    try {
      block()
    } finally {
      System.setOut(original)
    }
    return baos.toString()
  }

  private fun sampleHealth(healthy: Boolean = true) = MonorepoHealthDTO(
    rootProject = "my-monorepo",
    projects = listOf(
      ProjectHealthDTO(
        name = "core",
        path = "/projects/core",
        valid = healthy,
        errors = if (healthy) emptyList() else listOf("Missing required config"),
        warnings = if (healthy) emptyList() else listOf("Deprecated plugin"),
        taskCount = 5,
        lastBuildSuccess = true,
        lastBuildAgeSeconds = 300,
      ),
    ),
    healthyCount = if (healthy) 1 else 0,
    unhealthyCount = if (healthy) 0 else 1,
    timestamp = "2026-03-28T12:00:00Z",
  )

  @Test
  fun `printHealthDashboard shows healthy status with green checkmark`() {
    val formatter = OutputFormatter()
    val output = captureOutput { formatter.printHealthDashboard(sampleHealth(healthy = true)) }

    assertTrue(output.contains("✅"))
    assertTrue(output.contains("my-monorepo"))
    assertTrue(output.contains("core"))
    assertTrue(output.contains("5 tasks"))
  }

  @Test
  fun `printHealthDashboard shows errors and warnings for unhealthy project`() {
    val formatter = OutputFormatter()
    val output = captureOutput { formatter.printHealthDashboard(sampleHealth(healthy = false)) }

    assertTrue(output.contains("❌"))
    assertTrue(output.contains("Missing required config"))
    assertTrue(output.contains("Deprecated plugin"))
  }

  @Test
  fun `printHealthDashboard shows build age badge`() {
    val formatter = OutputFormatter()
    val output = captureOutput { formatter.printHealthDashboard(sampleHealth(healthy = true)) }

    assertTrue(output.contains("5m ago"))
  }

  @Test
  fun `printHealthDashboard shows build failed badge for failed cache entry`() {
    val formatter = OutputFormatter()
    val health = MonorepoHealthDTO(
      rootProject = "root",
      projects = listOf(
        ProjectHealthDTO(
          name = "api",
          path = "/api",
          valid = true,
          errors = emptyList(),
          warnings = emptyList(),
          taskCount = 3,
          lastBuildSuccess = false,
          lastBuildAgeSeconds = 3600,
        ),
      ),
      healthyCount = 1,
      unhealthyCount = 0,
      timestamp = "2026-03-28T12:00:00Z",
    )
    val output = captureOutput { formatter.printHealthDashboard(health) }

    assertTrue(output.contains("build failed"))
    assertTrue(output.contains("1h ago"))
  }

  @Test
  fun `printHealthDashboard renders JSON when jsonOutput is true`() {
    val formatter = OutputFormatter()
    val output = captureOutput { formatter.printHealthDashboard(sampleHealth(), jsonOutput = true) }

    assertTrue(output.contains("\"rootProject\""))
    assertTrue(output.contains("\"my-monorepo\""))
    assertTrue(output.contains("\"projects\""))
    assertFalse(output.contains("━"))
  }

  @Test
  fun `printHealthDashboard shows summary counts`() {
    val formatter = OutputFormatter()
    val output = captureOutput { formatter.printHealthDashboard(sampleHealth()) }

    assertTrue(output.contains("Projects: 1"))
    assertTrue(output.contains("Healthy: 1"))
    assertTrue(output.contains("Unhealthy: 0"))
  }
}
