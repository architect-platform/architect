package io.github.architectplatform.cli.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for TaskDTO data transfer object.
 */
class TaskDTOTest {
  @Test
  fun `TaskDTO should have correct id`() {
    val task = TaskDTO(id = "build")

    assertEquals("build", task.id)
  }

  @Test
  fun `TaskDTO toString should return id`() {
    val task = TaskDTO(id = "test")

    assertEquals("test", task.toString())
  }

  @Test
  fun `TaskDTO equality should work correctly`() {
    val task1 = TaskDTO(id = "build")
    val task2 = TaskDTO(id = "build")
    val task3 = TaskDTO(id = "test")

    assertEquals(task1, task2)
    assertNotEquals(task1, task3)
  }
}
