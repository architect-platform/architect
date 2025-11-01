package io.github.architectplatform.api.core.tasks.impl

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.TaskResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Tests for TaskWithArgs implementation.
 */
class TaskWithArgsTest {
  private fun createMockEnvironment(): Environment =
    object : Environment {
      override fun <T> service(type: Class<T>): T {
        throw UnsupportedOperationException("Mock environment")
      }

      override fun publish(event: Any) {}
    }

  private fun createMockProjectContext(): ProjectContext = ProjectContext(Paths.get("/test/project"), emptyMap())

  @Test
  fun `TaskWithArgs has correct id and description`() {
    val task =
      TaskWithArgs(
        id = "test-task",
        description = "A test task with args",
        phase = CoreWorkflow.BUILD,
      ) { _, _, _ -> TaskResult.success() }

    assertEquals("test-task", task.id)
    assertEquals("A test task with args", task.description())
  }

  @Test
  fun `TaskWithArgs has correct phase`() {
    val task =
      TaskWithArgs(
        id = "test-task",
        description = "A test task with args",
        phase = CoreWorkflow.BUILD,
      ) { _, _, _ -> TaskResult.success() }

    assertEquals(CoreWorkflow.BUILD, task.phase())
  }

  @Test
  fun `TaskWithArgs inherits dependencies from phase`() {
    val task =
      TaskWithArgs(
        id = "test-task",
        description = "A test task with args",
        phase = CoreWorkflow.BUILD,
      ) { _, _, _ -> TaskResult.success() }

    val dependencies = task.depends()
    assertTrue(dependencies.contains("verify"))
  }

  @Test
  fun `TaskWithArgs executes with empty args list`() {
    var receivedArgs: List<String>? = null
    val task =
      TaskWithArgs(
        id = "test-task",
        description = "A test task with args",
        phase = CoreWorkflow.BUILD,
      ) { _, _, args ->
        receivedArgs = args
        TaskResult.success()
      }

    task.execute(createMockEnvironment(), createMockProjectContext(), emptyList())

    assertNotNull(receivedArgs)
    assertTrue(receivedArgs!!.isEmpty())
  }

  @Test
  fun `TaskWithArgs receives and processes arguments`() {
    var receivedArgs: List<String>? = null
    val task =
      TaskWithArgs(
        id = "test-task",
        description = "A test task with args",
        phase = CoreWorkflow.BUILD,
      ) { _, _, args ->
        receivedArgs = args
        TaskResult.success("Processed ${args.size} arguments")
      }

    val args = listOf("arg1", "arg2", "arg3")
    val result = task.execute(createMockEnvironment(), createMockProjectContext(), args)

    assertEquals(args, receivedArgs)
    assertTrue(result.success)
    assertEquals("Processed 3 arguments", result.message)
  }

  @Test
  fun `TaskWithArgs can use arguments for logic`() {
    val task =
      TaskWithArgs(
        id = "greet",
        description = "Greets a person",
        phase = CoreWorkflow.RUN,
      ) { _, _, args ->
        val name = args.firstOrNull() ?: "World"
        TaskResult.success("Hello, $name!")
      }

    val result1 = task.execute(createMockEnvironment(), createMockProjectContext(), listOf("Alice"))
    assertEquals("Hello, Alice!", result1.message)

    val result2 = task.execute(createMockEnvironment(), createMockProjectContext(), emptyList())
    assertEquals("Hello, World!", result2.message)
  }

  @Test
  fun `TaskWithArgs can fail based on arguments`() {
    val task =
      TaskWithArgs(
        id = "validate",
        description = "Validates input",
        phase = CoreWorkflow.VERIFY,
      ) { _, _, args ->
        if (args.isEmpty()) {
          TaskResult.failure("No arguments provided")
        } else {
          TaskResult.success("Arguments validated")
        }
      }

    val result1 = task.execute(createMockEnvironment(), createMockProjectContext(), emptyList())
    assertFalse(result1.success)
    assertEquals("No arguments provided", result1.message)

    val result2 = task.execute(createMockEnvironment(), createMockProjectContext(), listOf("arg1"))
    assertTrue(result2.success)
    assertEquals("Arguments validated", result2.message)
  }

  @Test
  fun `TaskWithArgs receives environment and project context`() {
    val mockEnv = createMockEnvironment()
    val mockCtx = createMockProjectContext()
    var receivedEnv: Environment? = null
    var receivedCtx: ProjectContext? = null

    val task =
      TaskWithArgs(
        id = "test-task",
        description = "A test task with args",
        phase = CoreWorkflow.BUILD,
      ) { env, ctx, _ ->
        receivedEnv = env
        receivedCtx = ctx
        TaskResult.success()
      }

    task.execute(mockEnv, mockCtx, emptyList())

    assertSame(mockEnv, receivedEnv)
    assertSame(mockCtx, receivedCtx)
  }
}
