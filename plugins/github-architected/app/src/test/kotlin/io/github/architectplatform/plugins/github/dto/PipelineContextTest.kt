package io.github.architectplatform.plugins.github.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for PipelineContext data class.
 */
class PipelineContextTest {
  @Test
  fun `PipelineContext should be created with correct values`() {
    val pipeline = PipelineContext(name = "ci", type = "standard", path = "src/**", branch = "develop")

    assertEquals("ci", pipeline.name)
    assertEquals("standard", pipeline.type)
    assertEquals("src/**", pipeline.path)
    assertEquals("develop", pipeline.branch)
  }

  @Test
  fun `PipelineContext should have default values`() {
    val pipeline = PipelineContext(name = "build", type = "gradle")

    assertEquals("build", pipeline.name)
    assertEquals("gradle", pipeline.type)
    assertEquals("**", pipeline.path)
    assertEquals("main", pipeline.branch)
  }

  @Test
  fun `PipelineContext equality should work correctly`() {
    val pipeline1 = PipelineContext(name = "test", type = "maven")
    val pipeline2 = PipelineContext(name = "test", type = "maven")
    val pipeline3 = PipelineContext(name = "build", type = "maven")

    assertEquals(pipeline1, pipeline2)
    assertNotEquals(pipeline1, pipeline3)
  }
}
