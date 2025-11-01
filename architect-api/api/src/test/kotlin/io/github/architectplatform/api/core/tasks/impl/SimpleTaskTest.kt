package io.github.architectplatform.api.core.tasks.impl

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.TaskResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Tests for SimpleTask implementation.
 */
class SimpleTaskTest {
  private fun createMockEnvironment(): Environment =
    object : Environment {
      override fun <T> service(type: Class<T>): T {
        throw UnsupportedOperationException("Mock environment")
      }

      override fun publish(event: Any) {}
    }

  private fun createMockProjectContext(): ProjectContext = ProjectContext(Paths.get("/test/project"), emptyMap())

  @Test
  fun `SimpleTask has correct id and description`() {
    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { _, _ -> TaskResult.success() }

    assertEquals("test-task", task.id)
    assertEquals("A test task", task.description())
  }

  @Test
  fun `SimpleTask has correct phase`() {
    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { _, _ -> TaskResult.success() }

    assertEquals(CoreWorkflow.BUILD, task.phase())
  }

  @Test
  fun `SimpleTask inherits dependencies from phase`() {
    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { _, _ -> TaskResult.success() }

    val dependencies = task.depends()
    assertTrue(dependencies.contains("verify"))
  }

  @Test
  fun `SimpleTask executes successfully`() {
    var executed = false
    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { _, _ ->
        executed = true
        TaskResult.success("Executed successfully")
      }

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertTrue(executed)
    assertTrue(result.success)
    assertEquals("Executed successfully", result.message)
  }

  @Test
  fun `SimpleTask can fail during execution`() {
    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { _, _ ->
        TaskResult.failure("Task failed")
      }

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertFalse(result.success)
    assertEquals("Task failed", result.message)
  }

  @Test
  fun `SimpleTask receives environment and project context`() {
    val mockEnv = createMockEnvironment()
    val mockCtx = createMockProjectContext()
    var receivedEnv: Environment? = null
    var receivedCtx: ProjectContext? = null

    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { env, ctx ->
        receivedEnv = env
        receivedCtx = ctx
        TaskResult.success()
      }

    task.execute(mockEnv, mockCtx)

    assertSame(mockEnv, receivedEnv)
    assertSame(mockCtx, receivedCtx)
  }

  @Test
  fun `SimpleTask ignores args parameter`() {
    var argsReceived = false
    val task =
      SimpleTask(
        id = "test-task",
        description = "A test task",
        phase = CoreWorkflow.BUILD,
      ) { _, _ ->
        argsReceived = true
        TaskResult.success()
      }

    // SimpleTask doesn't use args, but execute is called with them
    val result = task.execute(createMockEnvironment(), createMockProjectContext(), listOf("arg1", "arg2"))

    assertTrue(argsReceived)
    assertTrue(result.success)
  }
}
