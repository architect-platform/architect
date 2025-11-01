package io.github.architectplatform.plugins.github.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for GithubContext data class.
 */
class GithubContextTest {
  @Test
  fun `GithubContext should have default values`() {
    val context = GithubContext()

    assertNotNull(context.release)
    assertTrue(context.release.enabled)
    assertTrue(context.pipelines.isEmpty())
    assertNotNull(context.deps)
    assertTrue(context.deps.enabled)
  }

  @Test
  fun `GithubContext should accept custom values`() {
    val release = GithubReleaseContext(enabled = false)
    val pipeline = PipelineContext(name = "ci", type = "standard", branch = "develop")
    val deps = DepsContext(enabled = false)
    val context = GithubContext(release = release, pipelines = listOf(pipeline), deps = deps)

    assertFalse(context.release.enabled)
    assertEquals(1, context.pipelines.size)
    assertEquals("ci", context.pipelines[0].name)
    assertFalse(context.deps.enabled)
  }

  @Test
  fun `GithubContext equality should work correctly`() {
    val context1 = GithubContext()
    val context2 = GithubContext()

    assertEquals(context1, context2)
  }
}
