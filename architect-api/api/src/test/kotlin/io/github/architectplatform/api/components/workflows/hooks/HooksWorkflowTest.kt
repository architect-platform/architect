package io.github.architectplatform.api.components.workflows.hooks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for HooksWorkflow phases.
 */
class HooksWorkflowTest {
  @Test
  fun `HooksWorkflow PRE_COMMIT has correct properties`() {
    val phase = HooksWorkflow.PRE_COMMIT

    assertEquals("pre-commit", phase.id)
    assertNull(phase.parent())
    assertEquals(emptyList<String>(), phase.depends())
  }

  @Test
  fun `HooksWorkflow PRE_PUSH has correct properties`() {
    val phase = HooksWorkflow.PRE_PUSH

    assertEquals("pre-push", phase.id)
    assertNull(phase.parent())
    assertEquals(emptyList<String>(), phase.depends())
  }

  @Test
  fun `HooksWorkflow COMMIT_MSG has correct properties`() {
    val phase = HooksWorkflow.COMMIT_MSG

    assertEquals("commit-msg", phase.id)
    assertNull(phase.parent())
    assertEquals(emptyList<String>(), phase.depends())
  }

  @Test
  fun `HooksWorkflow has three phases`() {
    assertEquals(3, HooksWorkflow.values().size)
  }

  @Test
  fun `HooksWorkflow all phases have no parent`() {
    HooksWorkflow.values().forEach { phase ->
      assertNull(phase.parent(), "Phase ${phase.id} should have no parent")
    }
  }

  @Test
  fun `HooksWorkflow all phases have no dependencies`() {
    HooksWorkflow.values().forEach { phase ->
      assertTrue(phase.depends().isEmpty(), "Phase ${phase.id} should have no dependencies")
    }
  }

  @Test
  fun `HooksWorkflow phases are in correct order`() {
    val phases = HooksWorkflow.values()

    assertEquals(HooksWorkflow.PRE_COMMIT, phases[0])
    assertEquals(HooksWorkflow.PRE_PUSH, phases[1])
    assertEquals(HooksWorkflow.COMMIT_MSG, phases[2])
  }

  @Test
  fun `HooksWorkflow phases have hyphenated ids`() {
    HooksWorkflow.values().forEach { phase ->
      assertTrue(phase.id.contains("-"), "Phase ${phase.id} should contain a hyphen")
    }
  }

  @Test
  fun `HooksWorkflow description uses default from Phase interface`() {
    HooksWorkflow.values().forEach { phase ->
      val description = phase.description()
      assertTrue(
        description.contains(phase.id),
        "Default description should contain phase id: $description",
      )
    }
  }
}
