package io.github.architectplatform.plugins.docker

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DockerContextTest {
  @Test
  fun `default context has sensible defaults`() {
    val ctx = DockerContext()
    assertEquals("", ctx.image)
    assertEquals("", ctx.registry)
    assertTrue(ctx.platforms.isEmpty())
    assertEquals("Dockerfile", ctx.dockerfile)
    assertTrue(ctx.buildArgs.isEmpty())
    assertEquals("docker-compose.yml", ctx.composeFile)
    assertTrue(ctx.enabled)
  }

  @Test
  fun `context stores all values`() {
    val ctx = DockerContext(
      image = "myapp:latest",
      registry = "ghcr.io/org",
      platforms = listOf("linux/amd64", "linux/arm64"),
      dockerfile = "Dockerfile.prod",
      buildArgs = mapOf("ENV" to "production"),
      composeFile = "compose.yaml",
      enabled = false,
    )
    assertEquals("myapp:latest", ctx.image)
    assertEquals("ghcr.io/org", ctx.registry)
    assertEquals(listOf("linux/amd64", "linux/arm64"), ctx.platforms)
    assertEquals("Dockerfile.prod", ctx.dockerfile)
    assertEquals(mapOf("ENV" to "production"), ctx.buildArgs)
    assertEquals("compose.yaml", ctx.composeFile)
    assertFalse(ctx.enabled)
  }

  @Test
  fun `context equality`() {
    val a = DockerContext(image = "test")
    val b = DockerContext(image = "test")
    assertEquals(a, b)
  }
}
