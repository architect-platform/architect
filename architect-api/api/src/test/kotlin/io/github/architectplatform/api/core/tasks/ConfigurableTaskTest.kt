package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Tests for ConfigurableTask implementation.
 */
class ConfigurableTaskTest {
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
  fun `ConfigurableTask has correct id and description`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        phase = CoreWorkflow.PUBLISH,
      ) { _, _, _, _ -> TaskResult.success() }

    assertEquals("deploy", task.id)
    assertEquals("Deploy application", task.description())
  }

  @Test
  fun `ConfigurableTask has correct phase`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        phase = CoreWorkflow.PUBLISH,
      ) { _, _, _, _ -> TaskResult.success() }

    assertEquals(CoreWorkflow.PUBLISH, task.phase())
  }

  @Test
  fun `ConfigurableTask can be standalone without phase`() {
    val task =
      ConfigurableTask(
        id = "custom",
        description = "Custom task",
      ) { _, _, _, _ -> TaskResult.success() }

    assertNull(task.phase())
  }

  @Test
  fun `ConfigurableTask returns configuration`() {
    val config = mapOf("env" to "production", "timeout" to "300")
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = config,
      ) { _, _, _, _ -> TaskResult.success() }

    assertEquals(config, task.config())
  }

  @Test
  fun `ConfigurableTask getConfig returns value`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = mapOf("env" to "production"),
      ) { _, _, _, _ -> TaskResult.success() }

    assertEquals("production", task.getConfig("env"))
  }

  @Test
  fun `ConfigurableTask getConfig returns default when key not found`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = mapOf("env" to "production"),
      ) { _, _, _, _ -> TaskResult.success() }

    assertEquals("default", task.getConfig("missing", "default"))
  }

  @Test
  fun `ConfigurableTask getConfig returns null when key not found and no default`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = mapOf("env" to "production"),
      ) { _, _, _, _ -> TaskResult.success() }

    assertNull(task.getConfig("missing"))
  }

  @Test
  fun `ConfigurableTask getRequiredConfig returns value`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = mapOf("env" to "production"),
      ) { _, _, _, _ -> TaskResult.success() }

    assertEquals("production", task.getRequiredConfig("env"))
  }

  @Test
  fun `ConfigurableTask getRequiredConfig throws when key not found`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = mapOf("env" to "production"),
      ) { _, _, _, _ -> TaskResult.success() }

    val exception = assertThrows(IllegalArgumentException::class.java) {
      task.getRequiredConfig("missing")
    }

    assertTrue(exception.message!!.contains("missing"))
    assertTrue(exception.message!!.contains("deploy"))
  }

  @Test
  fun `ConfigurableTask receives configuration in execute`() {
    val config = mapOf("env" to "production", "timeout" to "300")
    var receivedConfig: Map<String, String>? = null

    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = config,
      ) { _, _, cfg, _ ->
        receivedConfig = cfg
        TaskResult.success()
      }

    task.execute(createMockEnvironment(), createMockProjectContext())

    assertEquals(config, receivedConfig)
  }

  @Test
  fun `ConfigurableTask receives arguments in execute`() {
    var receivedArgs: List<String>? = null

    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
      ) { _, _, _, args ->
        receivedArgs = args
        TaskResult.success()
      }

    val testArgs = listOf("arg1", "arg2")
    task.execute(createMockEnvironment(), createMockProjectContext(), testArgs)

    assertEquals(testArgs, receivedArgs)
  }

  @Test
  fun `ConfigurableTask can succeed with message`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        config = mapOf("env" to "production"),
      ) { _, _, cfg, _ ->
        val env = cfg["env"]
        TaskResult.success("Deployed to $env")
      }

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertTrue(result.success)
    assertEquals("Deployed to production", result.message)
  }

  @Test
  fun `ConfigurableTask can fail with message`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
      ) { _, _, _, _ -> TaskResult.failure("Deployment failed") }

    val result = task.execute(createMockEnvironment(), createMockProjectContext())

    assertFalse(result.success)
    assertEquals("Deployment failed", result.message)
  }

  @Test
  fun `ConfigurableTask combines phase and custom dependencies`() {
    val task =
      ConfigurableTask(
        id = "deploy",
        description = "Deploy application",
        phase = CoreWorkflow.PUBLISH,
        customDependencies = listOf("verify-deployment", "backup"),
      ) { _, _, _, _ -> TaskResult.success() }

    val dependencies = task.depends()
    assertTrue(dependencies.contains("release")) // from phase
    assertTrue(dependencies.contains("verify-deployment")) // custom
    assertTrue(dependencies.contains("backup")) // custom
  }

  @Test
  fun `ConfigurableTask with only custom dependencies`() {
    val task =
      ConfigurableTask(
        id = "custom",
        description = "Custom task",
        customDependencies = listOf("dep1", "dep2"),
      ) { _, _, _, _ -> TaskResult.success() }

    val dependencies = task.depends()
    assertEquals(listOf("dep1", "dep2"), dependencies)
  }

  @Test
  fun `ConfigurableTask with empty configuration`() {
    val task =
      ConfigurableTask(
        id = "task",
        description = "Task with no config",
      ) { _, _, cfg, _ ->
        assertTrue(cfg.isEmpty())
        TaskResult.success()
      }

    val result = task.execute(createMockEnvironment(), createMockProjectContext())
    assertTrue(result.success)
  }

  @Test
  fun `ConfigurableTask removes duplicate dependencies`() {
    val task =
      ConfigurableTask(
        id = "task",
        description = "Test deduplication",
        phase = CoreWorkflow.PUBLISH, // depends on "release"
        customDependencies = listOf("release", "custom"), // "release" is duplicate
      ) { _, _, _, _ -> TaskResult.success() }

    val dependencies = task.depends()
    assertEquals(2, dependencies.size)
    assertEquals(1, dependencies.count { it == "release" })
  }
}
