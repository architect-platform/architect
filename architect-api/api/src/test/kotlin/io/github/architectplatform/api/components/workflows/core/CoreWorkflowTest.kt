package io.github.architectplatform.api.components.workflows.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for CoreWorkflow phases.
 */
class CoreWorkflowTest {
  @Test
  fun `CoreWorkflow INIT has correct properties`() {
    val phase = CoreWorkflow.INIT

    assertEquals("init", phase.id)
    assertEquals("Initialize the project", phase.description())
    assertNull(phase.parent())
    assertTrue(phase.depends().isEmpty())
  }

  @Test
  fun `CoreWorkflow LINT depends on INIT`() {
    val phase = CoreWorkflow.LINT

    assertEquals("lint", phase.id)
    assertEquals("Lint the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("init"), phase.depends())
  }

  @Test
  fun `CoreWorkflow VERIFY depends on LINT`() {
    val phase = CoreWorkflow.VERIFY

    assertEquals("verify", phase.id)
    assertEquals("Verify the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("lint"), phase.depends())
  }

  @Test
  fun `CoreWorkflow BUILD depends on VERIFY`() {
    val phase = CoreWorkflow.BUILD

    assertEquals("build", phase.id)
    assertEquals("Build the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("verify"), phase.depends())
  }

  @Test
  fun `CoreWorkflow RUN depends on BUILD`() {
    val phase = CoreWorkflow.RUN

    assertEquals("run", phase.id)
    assertEquals("Run the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("build"), phase.depends())
  }

  @Test
  fun `CoreWorkflow TEST depends on BUILD`() {
    val phase = CoreWorkflow.TEST

    assertEquals("test", phase.id)
    assertEquals("Test the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("build"), phase.depends())
  }

  @Test
  fun `CoreWorkflow RELEASE depends on TEST`() {
    val phase = CoreWorkflow.RELEASE

    assertEquals("release", phase.id)
    assertEquals("Release the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("test"), phase.depends())
  }

  @Test
  fun `CoreWorkflow PUBLISH depends on RELEASE`() {
    val phase = CoreWorkflow.PUBLISH

    assertEquals("publish", phase.id)
    assertEquals("Publish the project", phase.description())
    assertNull(phase.parent())
    assertEquals(listOf("release"), phase.depends())
  }

  @Test
  fun `CoreWorkflow phases form correct dependency chain`() {
    // Verify the complete dependency chain
    assertEquals(emptyList<String>(), CoreWorkflow.INIT.depends())
    assertEquals(listOf("init"), CoreWorkflow.LINT.depends())
    assertEquals(listOf("lint"), CoreWorkflow.VERIFY.depends())
    assertEquals(listOf("verify"), CoreWorkflow.BUILD.depends())
    assertEquals(listOf("build"), CoreWorkflow.RUN.depends())
    assertEquals(listOf("build"), CoreWorkflow.TEST.depends())
    assertEquals(listOf("test"), CoreWorkflow.RELEASE.depends())
    assertEquals(listOf("release"), CoreWorkflow.PUBLISH.depends())
  }

  @Test
  fun `CoreWorkflow all phases have no parent`() {
    CoreWorkflow.values().forEach { phase ->
      assertNull(phase.parent(), "Phase ${phase.id} should have no parent")
    }
  }

  @Test
  fun `CoreWorkflow has eight phases`() {
    assertEquals(8, CoreWorkflow.values().size)
  }

  @Test
  fun `CoreWorkflow phases are in correct order`() {
    val phases = CoreWorkflow.values()

    assertEquals(CoreWorkflow.INIT, phases[0])
    assertEquals(CoreWorkflow.LINT, phases[1])
    assertEquals(CoreWorkflow.VERIFY, phases[2])
    assertEquals(CoreWorkflow.BUILD, phases[3])
    assertEquals(CoreWorkflow.RUN, phases[4])
    assertEquals(CoreWorkflow.TEST, phases[5])
    assertEquals(CoreWorkflow.RELEASE, phases[6])
    assertEquals(CoreWorkflow.PUBLISH, phases[7])
  }
}
