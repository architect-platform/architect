package io.github.architectplatform.core.project.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConfigInterpolatorTest {

  @Test
  fun `interpolates env variables`() {
    // PATH is always set
    val config = mapOf("value" to "\${env.PATH}")
    val result = ConfigInterpolator.interpolate(config)
    assertNotNull(result["value"])
    assertNotEquals("\${env.PATH}", result["value"])
  }

  @Test
  fun `leaves unresolvable env var as-is when no default`() {
    val config = mapOf("value" to "\${env.ARCHITECT_INTERPOLATION_TEST_NONEXISTENT}")
    val result = ConfigInterpolator.interpolate(config)
    assertEquals("\${env.ARCHITECT_INTERPOLATION_TEST_NONEXISTENT}", result["value"])
  }

  @Test
  fun `uses default value for unset env var`() {
    val config = mapOf("port" to "\${env.ARCHITECT_INTERPOLATION_TEST_NONEXISTENT:8080}")
    val result = ConfigInterpolator.interpolate(config)
    assertEquals("8080", result["port"])
  }

  @Test
  fun `interpolates project variables`() {
    val config = mapOf(
      "project" to mapOf("name" to "my-app", "description" to "My App"),
      "greeting" to "Welcome to \${project.name}",
    )
    val result = ConfigInterpolator.interpolate(config)
    assertEquals("Welcome to my-app", result["greeting"])
  }

  @Test
  fun `interpolates project description`() {
    val config = mapOf(
      "project" to mapOf("name" to "app", "description" to "My App"),
      "title" to "\${project.description}",
    )
    val result = ConfigInterpolator.interpolate(config)
    assertEquals("My App", result["title"])
  }

  @Test
  fun `handles nested maps`() {
    val config = mapOf(
      "project" to mapOf("name" to "demo"),
      "server" to mapOf("host" to "localhost", "name" to "\${project.name}-server"),
    )
    val result = ConfigInterpolator.interpolate(config)
    @Suppress("UNCHECKED_CAST")
    val server = result["server"] as Map<String, Any>
    assertEquals("demo-server", server["name"])
    assertEquals("localhost", server["host"])
  }

  @Test
  fun `handles lists`() {
    val config = mapOf(
      "project" to mapOf("name" to "demo"),
      "tags" to listOf("\${project.name}", "latest"),
    )
    val result = ConfigInterpolator.interpolate(config)
    @Suppress("UNCHECKED_CAST")
    val tags = result["tags"] as List<String>
    assertEquals("demo", tags[0])
    assertEquals("latest", tags[1])
  }

  @Test
  fun `leaves non-string values untouched`() {
    val config = mapOf(
      "count" to 42,
      "enabled" to true,
      "ratio" to 3.14,
    )
    val result = ConfigInterpolator.interpolate(config)
    assertEquals(42, result["count"])
    assertEquals(true, result["enabled"])
    assertEquals(3.14, result["ratio"])
  }

  @Test
  fun `handles multiple placeholders in one string`() {
    val config = mapOf(
      "project" to mapOf("name" to "app", "description" to "My App"),
      "label" to "\${project.name} - \${project.description}",
    )
    val result = ConfigInterpolator.interpolate(config)
    assertEquals("app - My App", result["label"])
  }

  @Test
  fun `handles empty config`() {
    val result = ConfigInterpolator.interpolate(emptyMap())
    assertTrue(result.isEmpty())
  }

  @Test
  fun `project vars default when no project section`() {
    val config = mapOf("value" to "\${project.name:fallback}")
    val result = ConfigInterpolator.interpolate(config)
    assertEquals("fallback", result["value"])
  }
}
