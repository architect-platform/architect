package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Tests for CompositeTask implementation.
 */
class CompositeTaskTest {
  private fun createMockEnvironment(): Environment =
    object : Environment {
      override fun <T> service(type: Class<T>): T {
        throw UnsupportedOperationException("Mock environment")
      }

      override fun publish(event: Any) {}
    }

  private fun createMockProjectContext(): ProjectContext =
    ProjectContext(Paths.get("/test/project"), emptyMap())

  @Test
  fun `CompositeTask has correct id and description`() {
    val task =
      CompositeTask(
        id = "build-all",
        description = "Build all components",
        phase = CoreWorkflow.BUILD,
        children = listOf("compile", "package"),
      )

    assertEquals("build-all", task.id)
    assertEquals("Build all components", task.description())
  }

  @Test
  fun `CompositeTask has correct phase`() {
    val task =
      CompositeTask(
        id = "build-all",
        description = "Build all components",
        phase = CoreWorkflow.BUILD,
        children = listOf("compile", "package"),
      )

    assertEquals(CoreWorkflow.BUILD, task.phase())
  }

  @Test
  fun `CompositeTask can be standalone without phase`() {
    val task =
      CompositeTask(
        id = "custom-workflow",
        description = "Custom workflow",
        children = listOf("step1", "step2"),
      )

    assertNull(task.phase())
  }

  @Test
  fun `CompositeTask returns correct children`() {
    val children = listOf("compile", "package", "verify")
    val task =
      CompositeTask(
        id = "build-all",
        description = "Build all components",
        children = children,
      )

    assertEquals(children, task.children())
  }

  @Test
  fun `CompositeTask with no children returns empty list`() {
    val task =
      CompositeTask(
        id = "empty-task",
        description = "Task with no children",
      )

    assertTrue(task.children().isEmpty())
  }

  @Test
  fun `CompositeTask combines phase and custom dependencies`() {
    val task =
      CompositeTask(
        id = "build-all",
        description = "Build all components",
        phase = CoreWorkflow.BUILD,
        customDependencies = listOf("init-custom", "setup"),
      )

    val dependencies = task.depends()
    assertTrue(dependencies.contains("verify")) // from phase
    assertTrue(dependencies.contains("init-custom")) // custom
    assertTrue(dependencies.contains("setup")) // custom
  }

  @Test
  fun `CompositeTask with only custom dependencies`() {
    val task =
      CompositeTask(
        id = "custom-task",
        description = "Custom task",
        customDependencies = listOf("dep1", "dep2"),
      )

    val dependencies = task.depends()
    assertEquals(listOf("dep1", "dep2"), dependencies)
  }

  @Test
  fun `CompositeTask executes successfully without hooks`() {
    val task =
      CompositeTask(
        id = "simple-composite",
        description = "Simple composite task",
        children = listOf("child1", "child2"),
      )

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertTrue(result.success)
    assertNotNull(result.message)
    assertTrue(result.message!!.contains("simple-composite"))
  }

  @Test
  fun `CompositeTask executes before hook`() {
    var beforeExecuted = false
    val task =
      CompositeTask(
        id = "task-with-before",
        description = "Task with before hook",
        beforeChildren = { _, _ ->
          beforeExecuted = true
          TaskResult.success("Before hook executed")
        },
      )

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertTrue(beforeExecuted)
    assertTrue(result.success)
  }

  @Test
  fun `CompositeTask executes after hook`() {
    var afterExecuted = false
    val task =
      CompositeTask(
        id = "task-with-after",
        description = "Task with after hook",
        afterChildren = { _, _, childResults ->
          afterExecuted = true
          TaskResult.success("After hook executed with ${childResults.size} results")
        },
      )

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertTrue(afterExecuted)
    assertTrue(result.success)
  }

  @Test
  fun `CompositeTask fails if before hook fails`() {
    val task =
      CompositeTask(
        id = "failing-before",
        description = "Task with failing before hook",
        beforeChildren = { _, _ -> TaskResult.failure("Before hook failed") },
      )

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertFalse(result.success)
    assertNotNull(result.message)
    assertTrue(result.message!!.contains("before-children"))
  }

  @Test
  fun `CompositeTask fails if after hook fails`() {
    val task =
      CompositeTask(
        id = "failing-after",
        description = "Task with failing after hook",
        afterChildren = { _, _, _ -> TaskResult.failure("After hook failed") },
      )

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertFalse(result.success)
    assertNotNull(result.message)
    assertTrue(result.message!!.contains("after-children"))
  }

  @Test
  fun `CompositeTask can access environment and context in hooks`() {
    val mockEnv = createMockEnvironment()
    val mockCtx = createMockProjectContext()
    var receivedEnv: Environment? = null
    var receivedCtx: ProjectContext? = null

    val task =
      CompositeTask(
        id = "context-test",
        description = "Test context access",
        beforeChildren = { env, ctx ->
          receivedEnv = env
          receivedCtx = ctx
          TaskResult.success()
        },
      )

    task.execute(mockEnv, mockCtx)

    assertSame(mockEnv, receivedEnv)
    assertSame(mockCtx, receivedCtx)
  }

  @Test
  fun `CompositeTask removes duplicate dependencies`() {
    val task =
      CompositeTask(
        id = "dedupe-test",
        description = "Test dependency deduplication",
        // depends on "verify"
        phase = CoreWorkflow.BUILD,
        // "verify" is duplicate
        customDependencies = listOf("verify", "custom"),
      )

    val dependencies = task.depends()
    assertEquals(2, dependencies.size)
    assertEquals(1, dependencies.count { it == "verify" })
  }
}
