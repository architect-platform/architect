package io.github.architectplatform.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PluginDependencyResolverTest {

  @Test
  fun `sortByDependencies orders dependencies before dependents`() {
    val plugins = listOf(
      testPlugin("app", deps = listOf("core")),
      testPlugin("core"),
      testPlugin("ui", deps = listOf("app")),
    )

    val sorted = PluginDependencyResolver.sortByDependencies(plugins)
    assertEquals(listOf("core", "app", "ui"), sorted.map { it.id })
  }

  @Test
  fun `sortByDependencies throws when dependency is missing`() {
    val plugins = listOf(testPlugin("app", deps = listOf("core")))

    val error = assertFailsWith<IllegalStateException> {
      PluginDependencyResolver.sortByDependencies(plugins)
    }
    assert(error.message!!.contains("Missing plugin dependency"))
  }

  @Test
  fun `sortByDependencies throws on cycles`() {
    val plugins = listOf(
      testPlugin("a", deps = listOf("b")),
      testPlugin("b", deps = listOf("a")),
    )

    val error = assertFailsWith<IllegalStateException> {
      PluginDependencyResolver.sortByDependencies(plugins)
    }
    assert(error.message!!.contains("Circular plugin dependency"))
  }

  private fun testPlugin(id: String, deps: List<String> = emptyList()): ArchitectPlugin<Map<String, Any>> =
    object : ArchitectPlugin<Map<String, Any>> {
      override val id: String = id
      override val contextKey: String = id
      override val ctxClass: Class<Map<String, Any>> = Map::class.java as Class<Map<String, Any>>
      override var context: Map<String, Any> = emptyMap()
      override fun register(registry: TaskRegistry) = Unit
      override fun dependencies(): List<String> = deps
    }
}
