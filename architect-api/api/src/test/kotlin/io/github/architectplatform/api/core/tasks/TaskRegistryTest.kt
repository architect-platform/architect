package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.impl.SimpleTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for the [TaskRegistry] contract using a simple in-memory implementation.
 */
class TaskRegistryTest {

  private lateinit var registry: TaskRegistry

  @BeforeEach
  fun setUp() {
    registry = SimpleTaskRegistry()
  }

  @Test
  fun `add and get returns registered task`() {
    val task = simpleTask("build", "Compile sources")
    registry.add(task)

    val found = registry.get("build")
    assertNotNull(found)
    assertEquals("build", found!!.id)
    assertEquals("Compile sources", found.description())
  }

  @Test
  fun `get returns null for unknown id`() {
    assertNull(registry.get("nonexistent"))
  }

  @Test
  fun `all returns empty list when no tasks registered`() {
    assertTrue(registry.all().isEmpty())
  }

  @Test
  fun `all returns all registered tasks in insertion order`() {
    registry.add(simpleTask("a", "Task A"))
    registry.add(simpleTask("b", "Task B"))
    registry.add(simpleTask("c", "Task C"))

    val all = registry.all()
    assertEquals(3, all.size)
    assertEquals(listOf("a", "b", "c"), all.map { it.id })
  }

  @Test
  fun `add throws on duplicate id`() {
    registry.add(simpleTask("dup", "First"))

    val ex = assertThrows(IllegalArgumentException::class.java) {
      registry.add(simpleTask("dup", "Second"))
    }
    assertTrue(ex.message!!.contains("dup"))
  }

  @Test
  fun `add multiple tasks and retrieve individually`() {
    registry.add(simpleTask("lint", "Lint code"))
    registry.add(simpleTask("test", "Run tests"))
    registry.add(simpleTask("deploy", "Ship it"))

    assertNotNull(registry.get("lint"))
    assertNotNull(registry.get("test"))
    assertNotNull(registry.get("deploy"))
    assertNull(registry.get("build"))
  }

  // ─── helpers ──────────────────────────────────────────────────────────────

  private fun simpleTask(id: String, description: String): Task =
    SimpleTask(id = id, description = description) { _, _ -> TaskResult.success(id) }

  /**
   * Minimal TaskRegistry implementation for contract testing.
   */
  private class SimpleTaskRegistry : TaskRegistry {
    private val tasks = linkedMapOf<String, Task>()

    override fun add(task: Task) {
      require(task.id !in tasks) { "Task '${task.id}' already registered" }
      tasks[task.id] = task
    }

    override fun get(id: String): Task? = tasks[id]

    override fun all(): List<Task> = tasks.values.toList()
  }
}
