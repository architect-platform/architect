package io.github.architectplatform.api.components.workflows.code

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for CodeWorkflow phases.
 */
class CodeWorkflowTest {
  @Test
  fun `CodeWorkflow INIT has correct properties`() {
    val phase = CodeWorkflow.INIT

    assertEquals("CODE-init", phase.id)
    assertTrue(phase.description().contains("init"))
    assertEquals(CoreWorkflow.INIT, phase.parent())
  }

  @Test
  fun `CodeWorkflow LINT has correct properties`() {
    val phase = CodeWorkflow.LINT

    assertEquals("CODE-lint", phase.id)
    assertTrue(phase.description().contains("lint"))
    assertEquals(CoreWorkflow.LINT, phase.parent())
  }

  @Test
  fun `CodeWorkflow VERIFY has correct properties`() {
    val phase = CodeWorkflow.VERIFY

    assertEquals("CODE-verify", phase.id)
    assertTrue(phase.description().contains("verify"))
    assertEquals(CoreWorkflow.VERIFY, phase.parent())
  }

  @Test
  fun `CodeWorkflow BUILD has correct properties`() {
    val phase = CodeWorkflow.BUILD

    assertEquals("CODE-build", phase.id)
    assertTrue(phase.description().contains("build"))
    assertEquals(CoreWorkflow.BUILD, phase.parent())
  }

  @Test
  fun `CodeWorkflow TEST has correct properties`() {
    val phase = CodeWorkflow.TEST

    assertEquals("CODE-test", phase.id)
    assertTrue(phase.description().contains("test"))
    assertEquals(CoreWorkflow.TEST, phase.parent())
  }

  @Test
  fun `CodeWorkflow RUN has correct properties`() {
    val phase = CodeWorkflow.RUN

    assertEquals("CODE-run", phase.id)
    assertTrue(phase.description().contains("run"))
    assertEquals(CoreWorkflow.RUN, phase.parent())
  }

  @Test
  fun `CodeWorkflow RELEASE has correct properties`() {
    val phase = CodeWorkflow.RELEASE

    assertEquals("CODE-release", phase.id)
    assertTrue(phase.description().contains("release"))
    assertEquals(CoreWorkflow.RELEASE, phase.parent())
  }

  @Test
  fun `CodeWorkflow PUBLISH has correct properties`() {
    val phase = CodeWorkflow.PUBLISH

    assertEquals("CODE-publish", phase.id)
    assertTrue(phase.description().contains("publish"))
    assertEquals(CoreWorkflow.PUBLISH, phase.parent())
  }

  @Test
  fun `CodeWorkflow phases map to CoreWorkflow phases`() {
    assertEquals(CoreWorkflow.INIT, CodeWorkflow.INIT.parent())
    assertEquals(CoreWorkflow.LINT, CodeWorkflow.LINT.parent())
    assertEquals(CoreWorkflow.VERIFY, CodeWorkflow.VERIFY.parent())
    assertEquals(CoreWorkflow.BUILD, CodeWorkflow.BUILD.parent())
    assertEquals(CoreWorkflow.TEST, CodeWorkflow.TEST.parent())
    assertEquals(CoreWorkflow.RUN, CodeWorkflow.RUN.parent())
    assertEquals(CoreWorkflow.RELEASE, CodeWorkflow.RELEASE.parent())
    assertEquals(CoreWorkflow.PUBLISH, CodeWorkflow.PUBLISH.parent())
  }

  @Test
  fun `CodeWorkflow has eight phases`() {
    assertEquals(8, CodeWorkflow.values().size)
  }

  @Test
  fun `CodeWorkflow phases have CODE prefix`() {
    CodeWorkflow.values().forEach { phase ->
      assertTrue(phase.id.startsWith("CODE-"), "Phase ${phase.id} should start with CODE-")
    }
  }

  @Test
  fun `CodeWorkflow phases are in correct order`() {
    val phases = CodeWorkflow.values()

    assertEquals(CodeWorkflow.INIT, phases[0])
    assertEquals(CodeWorkflow.LINT, phases[1])
    assertEquals(CodeWorkflow.VERIFY, phases[2])
    assertEquals(CodeWorkflow.BUILD, phases[3])
    assertEquals(CodeWorkflow.TEST, phases[4])
    assertEquals(CodeWorkflow.RUN, phases[5])
    assertEquals(CodeWorkflow.RELEASE, phases[6])
    assertEquals(CodeWorkflow.PUBLISH, phases[7])
  }

  @Test
  fun `CodeWorkflow description includes parent phase id`() {
    CodeWorkflow.values().forEach { phase ->
      val description = phase.description()
      val parent = phase.parent()
      assertNotNull(parent)
      assertTrue(
        description.contains(parent.id),
        "Description should contain parent phase id: $description",
      )
    }
  }
}
