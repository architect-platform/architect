package io.github.architectplatform.core.tasks.infrastructure

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InMemoryTaskRegistryTest {
  @Test
  fun `should resolve aliases groups and wildcards to direct tasks`() {
    val registry = InMemoryTaskRegistry()
    registry.add(simpleTask("frontend-build"))
    registry.add(simpleTask("backend-build"))
    registry.addAlias("build:frontend", "frontend-build")
    registry.addAlias("build:backend", "backend-build")
    registry.addGroup("build", listOf("frontend-build", "backend-build"))

    assertEquals(listOf("frontend-build"), registry.resolve("build:frontend").map { it.id })
    assertEquals(
      listOf("frontend-build", "backend-build"),
      registry.resolve("build").map { it.id },
    )
    assertEquals(
      listOf("frontend-build", "backend-build"),
      registry.resolve("build:*").map { it.id },
    )
  }

  @Test
  fun `should register synthetic group tasks`() {
    val registry = InMemoryTaskRegistry()
    registry.add(simpleTask("frontend-build"))
    registry.add(simpleTask("backend-build"))

    registry.addGroup("build", listOf("frontend-build", "backend-build"))

    val group = requireNotNull(registry.get("build"))
    assertEquals(listOf("backend-build", "frontend-build"), group.depends().sorted())
    assertNull(registry.get("deploy"))
  }

  @Test
  fun `should preserve direct task IDs when group ID matches`() {
    val registry = InMemoryTaskRegistry()
    registry.add(simpleTask("build"))
    registry.add(simpleTask("frontend-build"))
    registry.addGroup("build", listOf("frontend-build"))

    assertEquals(listOf("build"), registry.resolve("build").map { it.id })
    assertEquals(listOf("frontend-build"), registry.resolve("build:*").map { it.id })
  }

  @Test
  fun `should resolve hyphen alias for namespaced tasks without overriding direct ids`() {
    val registry = InMemoryTaskRegistry()
    registry.add(simpleTask("git:commit"))

    assertEquals(listOf("git:commit"), registry.resolve("git-commit").map { it.id })

    registry.add(simpleTask("git-commit"))
    assertEquals(listOf("git-commit"), registry.resolve("git-commit").map { it.id })
  }

  private fun simpleTask(id: String) =
    SimpleTask(id = id, description = id) { _, _ -> TaskResult.success(id) }
}
