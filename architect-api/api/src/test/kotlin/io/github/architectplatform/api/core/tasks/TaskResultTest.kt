package io.github.architectplatform.api.core.tasks

import java.time.Duration
import java.time.Instant
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
    assertNull(result.metadata)
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
    assertNull(result.metadata)
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

  @Test
  fun `success with metadata creates TaskResult with metadata`() {
    val now = Instant.now()
    val metadata = TaskMetadata(
      duration = Duration.ofMillis(1500),
      exitCode = 0,
      startedAt = now.minusMillis(1500),
      finishedAt = now,
      executorInfo = "BashCommandExecutor",
    )
    val result = TaskResult.success("Build completed", metadata = metadata)

    assertTrue(result.success)
    assertEquals("Build completed", result.message)
    assertNotNull(result.metadata)
    assertEquals(Duration.ofMillis(1500), result.metadata!!.duration)
    assertEquals(0, result.metadata!!.exitCode)
    assertEquals("BashCommandExecutor", result.metadata!!.executorInfo)
    assertNotNull(result.metadata!!.startedAt)
    assertNotNull(result.metadata!!.finishedAt)
  }

  @Test
  fun `failure with metadata creates TaskResult with metadata`() {
    val metadata = TaskMetadata(
      duration = Duration.ofMillis(500),
      exitCode = 1,
      executorInfo = "BashCommandExecutor",
    )
    val result = TaskResult.failure("Build failed", metadata = metadata)

    assertFalse(result.success)
    assertEquals("Build failed", result.message)
    assertNotNull(result.metadata)
    assertEquals(1, result.metadata!!.exitCode)
    assertEquals(Duration.ofMillis(500), result.metadata!!.duration)
  }

  @Test
  fun `metadata defaults to null for backward compatibility`() {
    val success = TaskResult.success("Done")
    val failure = TaskResult.failure("Failed")

    assertNull(success.metadata)
    assertNull(failure.metadata)
  }

  @Test
  fun `TaskMetadata has proper equality`() {
    val meta1 = TaskMetadata(duration = Duration.ofMillis(100), exitCode = 0)
    val meta2 = TaskMetadata(duration = Duration.ofMillis(100), exitCode = 0)

    assertEquals(meta1, meta2)
  }

  @Test
  fun `TaskMetadata allows all fields to be null`() {
    val metadata = TaskMetadata()

    assertNull(metadata.duration)
    assertNull(metadata.exitCode)
    assertNull(metadata.startedAt)
    assertNull(metadata.finishedAt)
    assertNull(metadata.executorInfo)
  }

  @Test
  fun `results with same content but different metadata are not equal`() {
    val meta1 = TaskMetadata(exitCode = 0)
    val meta2 = TaskMetadata(exitCode = 1)
    val result1 = TaskResult.success("Done", metadata = meta1)
    val result2 = TaskResult.success("Done", metadata = meta2)

    assertNotEquals(result1, result2)
  }
}
