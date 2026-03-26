package io.github.architectplatform.api.core.tasks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TaskNotFoundExceptionTest {

  @Test
  fun `includes task and project name in message`() {
    val ex = TaskNotFoundException("build", "my-app")
    assertTrue(ex.message!!.contains("'build'"))
    assertTrue(ex.message!!.contains("'my-app'"))
  }

  @Test
  fun `suggests similar task names`() {
    val ex = TaskNotFoundException("buld", "my-app", listOf("build", "test", "lint"))
    assertTrue(ex.message!!.contains("Did you mean"))
    assertTrue(ex.message!!.contains("'build'"))
  }

  @Test
  fun `shows available tasks when no similar names`() {
    val ex = TaskNotFoundException("xyz", "my-app", listOf("build", "test", "lint"))
    assertTrue(ex.message!!.contains("Available tasks"))
  }

  @Test
  fun `handles empty task list`() {
    val ex = TaskNotFoundException("build", "my-app", emptyList())
    assertTrue(ex.message!!.contains("No tasks are registered"))
  }

  @Test
  fun `levenshtein distance is correct`() {
    assertEquals(0, TaskNotFoundException.levenshtein("abc", "abc"))
    assertEquals(1, TaskNotFoundException.levenshtein("abc", "abd"))
    assertEquals(3, TaskNotFoundException.levenshtein("abc", "def"))
    assertEquals(1, TaskNotFoundException.levenshtein("build", "buld"))
  }

  @Test
  fun `findSimilar returns closest matches`() {
    val similar = TaskNotFoundException.findSimilar(
      "buld", listOf("build", "test", "lint", "bundle"), maxSuggestions = 2
    )
    assertTrue(similar.contains("build"))
    assertTrue(similar.size <= 2)
  }

  @Test
  fun `extends IllegalArgumentException`() {
    val ex = TaskNotFoundException("x", "p")
    assertTrue(ex is IllegalArgumentException)
  }
}
