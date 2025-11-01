package io.github.architectplatform.cli.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for TaskResultDTO data transfer object.
 */
class TaskResultDTOTest {
  @Test
  fun `TaskResultDTO should represent successful result`() {
    val result = TaskResultDTO(success = true, message = "Task completed")

    assertTrue(result.success)
    assertEquals("Task completed", result.message)
    assertTrue(result.subResults.isEmpty())
  }

  @Test
  fun `TaskResultDTO should represent failed result`() {
    val result = TaskResultDTO(success = false, message = "Task failed")

    assertFalse(result.success)
    assertEquals("Task failed", result.message)
    assertTrue(result.subResults.isEmpty())
  }

  @Test
  fun `TaskResultDTO should support nested sub-results`() {
    val subResult1 = TaskResultDTO(success = true, message = "Sub-task 1")
    val subResult2 = TaskResultDTO(success = false, message = "Sub-task 2")
    val result =
        TaskResultDTO(
            success = false, message = "Main task", subResults = listOf(subResult1, subResult2))

    assertFalse(result.success)
    assertEquals(2, result.subResults.size)
    assertTrue(result.subResults[0].success)
    assertFalse(result.subResults[1].success)
  }

  @Test
  fun `TaskResultDTO toString should render tree structure`() {
    val result = TaskResultDTO(success = true, message = "Test task")
    val output = result.toString()

    assertTrue(output.contains("✅"))
    assertTrue(output.contains("Test task"))
  }

  @Test
  fun `TaskResultDTO toString should render nested structure`() {
    val subResult = TaskResultDTO(success = true, message = "Sub-task")
    val result =
        TaskResultDTO(success = true, message = "Main task", subResults = listOf(subResult))
    val output = result.toString()

    assertTrue(output.contains("Main task"))
    assertTrue(output.contains("Sub-task"))
    assertTrue(output.contains("└──") || output.contains("├──"))
  }

  @Test
  fun `TaskResultDTO should show failure icon for failed tasks`() {
    val result = TaskResultDTO(success = false, message = "Failed task")
    val output = result.toString()

    assertTrue(output.contains("❌"))
    assertTrue(output.contains("Failed task"))
  }
}
