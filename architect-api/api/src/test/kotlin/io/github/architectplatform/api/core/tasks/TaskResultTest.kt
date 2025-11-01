package io.github.architectplatform.api.core.tasks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for TaskResult interface and its companion object factory methods.
 */
class TaskResultTest {
  @Test
  fun `success creates a successful TaskResult`() {
    val result = TaskResult.success()

    assertTrue(result.success)
    assertNull(result.message)
    assertTrue(result.results.isEmpty())
  }

  @Test
  fun `success with message creates a successful TaskResult with message`() {
    val message = "Task completed successfully"
    val result = TaskResult.success(message)

    assertTrue(result.success)
    assertEquals(message, result.message)
    assertTrue(result.results.isEmpty())
  }

  @Test
  fun `success with sub-results creates a successful TaskResult with nested results`() {
    val subResult1 = TaskResult.success("Sub-task 1 completed")
    val subResult2 = TaskResult.success("Sub-task 2 completed")
    val result = TaskResult.success("All sub-tasks completed", listOf(subResult1, subResult2))

    assertTrue(result.success)
    assertEquals("All sub-tasks completed", result.message)
    assertEquals(2, result.results.size)
    assertEquals(subResult1, result.results[0])
    assertEquals(subResult2, result.results[1])
  }

  @Test
  fun `failure creates a failed TaskResult`() {
    val result = TaskResult.failure()

    assertFalse(result.success)
    assertNull(result.message)
    assertTrue(result.results.isEmpty())
  }

  @Test
  fun `failure with message creates a failed TaskResult with message`() {
    val message = "Task failed due to error"
    val result = TaskResult.failure(message)

    assertFalse(result.success)
    assertEquals(message, result.message)
    assertTrue(result.results.isEmpty())
  }

  @Test
  fun `failure with sub-results creates a failed TaskResult with nested results`() {
    val subResult1 = TaskResult.failure("Sub-task 1 failed")
    val subResult2 = TaskResult.success("Sub-task 2 succeeded")
    val result = TaskResult.failure("One or more sub-tasks failed", listOf(subResult1, subResult2))

    assertFalse(result.success)
    assertEquals("One or more sub-tasks failed", result.message)
    assertEquals(2, result.results.size)
  }

  @Test
  fun `TaskResultImpl is a data class with proper equality`() {
    val result1 = TaskResult.success("Test message")
    val result2 = TaskResult.success("Test message")

    assertEquals(result1, result2)
  }

  @Test
  fun `nested results can be accessed recursively`() {
    val deepResult = TaskResult.success("Deep result")
    val midResult = TaskResult.success("Mid result", listOf(deepResult))
    val topResult = TaskResult.success("Top result", listOf(midResult))

    assertTrue(topResult.success)
    assertEquals(1, topResult.results.size)
    assertEquals(1, topResult.results[0].results.size)
    assertEquals(deepResult, topResult.results[0].results[0])
  }
}
