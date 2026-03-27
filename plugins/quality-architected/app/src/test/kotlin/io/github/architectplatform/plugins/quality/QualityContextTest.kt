package io.github.architectplatform.plugins.quality

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QualityContextTest {

  @Test
  fun `default context has sensible defaults`() {
    val ctx = QualityContext()
    assertTrue(ctx.tools.isEmpty())
    assertEquals(QualityGates(), ctx.gates)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `default gates have expected thresholds`() {
    val gates = QualityGates()
    assertEquals(80, gates.coverage)
    assertEquals(3, gates.duplications)
    assertEquals(0, gates.bugs)
    assertEquals(0, gates.vulnerabilities)
  }

  @Test
  fun `context stores all values`() {
    val tools = listOf(
      QualityTool(name = "sonarqube", url = "https://sonar.example.com", projectKey = "my-project"),
      QualityTool(name = "codeclimate"),
    )
    val gates = QualityGates(coverage = 90, duplications = 5, bugs = 1, vulnerabilities = 0)
    val ctx = QualityContext(tools = tools, gates = gates, enabled = false)

    assertEquals(2, ctx.tools.size)
    assertEquals("sonarqube", ctx.tools[0].name)
    assertEquals("https://sonar.example.com", ctx.tools[0].url)
    assertEquals("my-project", ctx.tools[0].projectKey)
    assertEquals("codeclimate", ctx.tools[1].name)
    assertEquals(90, ctx.gates.coverage)
    assertEquals(5, ctx.gates.duplications)
    assertEquals(1, ctx.gates.bugs)
    assertEquals(0, ctx.gates.vulnerabilities)
    assertFalse(ctx.enabled)
  }

  @Test
  fun `context equality`() {
    val a = QualityContext(enabled = false)
    val b = QualityContext(enabled = false)
    assertEquals(a, b)
  }

  @Test
  fun `quality tool stores optional fields`() {
    val tool = QualityTool(
      name = "sonarqube",
      url = "https://sonar.example.com",
      projectKey = "my-project",
      configFile = "sonar-project.properties",
    )
    assertEquals("sonarqube", tool.name)
    assertEquals("https://sonar.example.com", tool.url)
    assertEquals("my-project", tool.projectKey)
    assertEquals("sonar-project.properties", tool.configFile)
  }

  @Test
  fun `quality tool defaults optional fields to null`() {
    val tool = QualityTool(name = "detekt")
    assertEquals("detekt", tool.name)
    assertEquals(null, tool.url)
    assertEquals(null, tool.projectKey)
    assertEquals(null, tool.configFile)
  }

  @Test
  fun `gates equality`() {
    val a = QualityGates(coverage = 70, duplications = 2, bugs = 1, vulnerabilities = 0)
    val b = QualityGates(coverage = 70, duplications = 2, bugs = 1, vulnerabilities = 0)
    assertEquals(a, b)
  }
}
