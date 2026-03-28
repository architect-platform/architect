package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Abstract JUnit 5 contract test suite for [ArchitectPlugin] implementations.
 *
 * Extend this class and implement the required abstract members to automatically verify
 * that your plugin conforms to the ArchitectPlugin protocol. Each @Test method validates
 * a specific aspect of the contract.
 *
 * Minimal usage:
 * ```kotlin
 * class MyPluginContractTest : ArchitectPluginContractTestSuite<MyContext>() {
 *   override fun createPlugin() = MyPlugin()
 *   override fun expectedTaskIds() = setOf("my-task")
 * }
 * ```
 */
abstract class ArchitectPluginContractTestSuite<C : Any> {
  /** Create a fresh plugin instance. Called per test. */
  abstract fun createPlugin(): ArchitectPlugin<C>

  /** Task IDs the plugin must register. */
  abstract fun expectedTaskIds(): Set<String>

  /** Services needed for task execution (e.g. CommandExecutor). */
  open fun services(): Map<Class<*>, Any> = emptyMap()

  /** Plugin configuration to apply via init(). */
  open fun pluginConfig(): Any? = null

  /** Project-level configuration map. */
  open fun projectConfig(): Map<String, Any> = emptyMap()

  /** Task IDs that should execute successfully in test context. */
  open fun executableTaskIds(): List<String> = emptyList()

  @Test
  fun `plugin id must not be blank`() {
    assertFalse(createPlugin().id.isBlank(), "Plugin id must not be blank")
  }

  @Test
  fun `plugin contextKey must not be blank`() {
    assertFalse(createPlugin().contextKey.isBlank(), "Plugin contextKey must not be blank")
  }

  @Test
  fun `plugin ctxClass must not be null`() {
    assertNotNull(createPlugin().ctxClass, "Plugin ctxClass must not be null")
  }

  @Test
  fun `plugin context must match ctxClass`() {
    val plugin = createPlugin()
    val ctx = plugin.context
    assertTrue(
      plugin.ctxClass.isInstance(ctx) || plugin.ctxClass == Unit::class.java || ctx == Unit,
      "Context must be assignable to ${plugin.ctxClass.name} but was ${ctx.javaClass.name}",
    )
  }

  @Test
  fun `plugin must register at least one task`() {
    assertTrue(createTestKit().tasks().isNotEmpty(), "Plugin must register at least one task")
  }

  @Test
  fun `all registered tasks must have non-blank ids`() {
    createTestKit().tasks().forEach { task ->
      assertFalse(task.id.isBlank(), "Task id must not be blank")
    }
  }

  @Test
  fun `all registered tasks must have non-blank descriptions`() {
    createTestKit().tasks().forEach { task ->
      assertFalse(task.description().isBlank(), "Task '${task.id}' must have a non-blank description")
    }
  }

  @Test
  fun `all registered task ids must be unique`() {
    val ids = createTestKit().tasks().map { it.id }
    assertEquals(
      ids.size,
      ids.toSet().size,
      "Duplicate task IDs: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}",
    )
  }

  @Test
  fun `plugin registers all expected tasks`() {
    val expected = expectedTaskIds()
    if (expected.isEmpty()) return
    val actual = createTestKit().tasks().map { it.id }.toSet()
    val missing = expected - actual
    assertTrue(missing.isEmpty(), "Missing expected tasks: ${missing.sorted().joinToString(", ")}")
  }

  @Test
  fun `init is idempotent`() {
    val config = pluginConfig() ?: return
    val plugin = createPlugin()
    val kit = ArchitectPluginTestKit(plugin)
    kit.configure(config)
    val contextAfterFirst = plugin.context
    kit.configure(config)
    assertEquals(contextAfterFirst, plugin.context, "init() should be idempotent")
  }

  @Test
  fun `re-registration produces same tasks`() {
    val first = createTestKit().tasks().map { it.id }.sorted()
    val second = createTestKit().tasks().map { it.id }.sorted()
    assertEquals(first, second, "Re-registration should produce the same tasks")
  }

  @Test
  fun `executable tasks complete successfully`() {
    val ids = executableTaskIds()
    if (ids.isEmpty()) return
    val kit = createTestKit()
    ids.forEach { taskId ->
      val result = kit.executeTask(taskId)
      assertTrue(result.success, "Task '$taskId' failed: ${result.message}")
    }
  }

  @Test
  fun `full contract verification passes`() {
    ArchitectPluginContract(::createPlugin).verify(
      ArchitectPluginContract.Verification(
        config = pluginConfig(),
        expectedTaskIds = expectedTaskIds(),
        executableTaskIds = executableTaskIds(),
        projectConfig = projectConfig(),
        services = services(),
      ),
    )
  }

  private fun createTestKit(): ArchitectPluginTestKit<C> {
    val kit =
      ArchitectPluginTestKit(createPlugin())
        .withProjectConfig(projectConfig())
    services().forEach { (type, service) ->
      @Suppress("UNCHECKED_CAST")
      kit.withService(type as Class<Any>, service)
    }
    pluginConfig()?.let { kit.configure(it) }
    return kit
  }
}
