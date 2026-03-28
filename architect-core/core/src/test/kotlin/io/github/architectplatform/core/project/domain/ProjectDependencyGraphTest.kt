package io.github.architectplatform.core.project.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectDependencyGraphTest {

  private fun graph(
    projects: Set<String>,
    dependencies: Map<String, Set<String>> = emptyMap(),
  ): ProjectDependencyGraph =
    ProjectDependencyGraph(
      projects = projects,
      dependencies = projects.associateWith { dependencies[it].orEmpty() },
    )

  // ── detectCycles ────────────────────────────────────────────────────────

  @Test
  fun `no cycles returns empty list`() {
    val g = graph(
      setOf("a", "b", "c"),
      mapOf("b" to setOf("a"), "c" to setOf("b")),
    )

    assertTrue(g.detectCycles().isEmpty())
  }

  @Test
  fun `two-node cycle is detected`() {
    val g = graph(
      setOf("a", "b"),
      mapOf("a" to setOf("b"), "b" to setOf("a")),
    )

    val cycles = g.detectCycles()
    assertTrue(cycles.isNotEmpty())
    val cycleNodes = cycles.flatten().toSet()
    assertTrue("a" in cycleNodes)
    assertTrue("b" in cycleNodes)
  }

  @Test
  fun `three-node cycle is detected`() {
    val g = graph(
      setOf("a", "b", "c"),
      mapOf("a" to setOf("b"), "b" to setOf("c"), "c" to setOf("a")),
    )

    val cycles = g.detectCycles()
    assertTrue(cycles.isNotEmpty())
    val cycleNodes = cycles.flatten().toSet()
    assertEquals(setOf("a", "b", "c"), cycleNodes)
  }

  @Test
  fun `acyclic diamond has no cycles`() {
    val g = graph(
      setOf("shared", "a", "b", "app"),
      mapOf(
        "a" to setOf("shared"),
        "b" to setOf("shared"),
        "app" to setOf("a", "b"),
      ),
    )

    assertTrue(g.detectCycles().isEmpty())
  }

  // ── sharedDependencies ──────────────────────────────────────────────────

  @Test
  fun `no shared dependencies returns empty set`() {
    val g = graph(
      setOf("a", "b", "c"),
      mapOf("b" to setOf("a")),
    )

    assertTrue(g.sharedDependencies().isEmpty())
  }

  @Test
  fun `dependency used by two projects is shared`() {
    val g = graph(
      setOf("shared", "a", "b"),
      mapOf("a" to setOf("shared"), "b" to setOf("shared")),
    )

    assertEquals(setOf("shared"), g.sharedDependencies())
  }

  @Test
  fun `dependency used by one project is not shared`() {
    val g = graph(
      setOf("shared", "a", "b"),
      mapOf("a" to setOf("shared")),
    )

    assertTrue(g.sharedDependencies().isEmpty())
  }

  @Test
  fun `multiple shared dependencies are all returned`() {
    val g = graph(
      setOf("core", "utils", "a", "b"),
      mapOf(
        "a" to setOf("core", "utils"),
        "b" to setOf("core", "utils"),
      ),
    )

    assertEquals(setOf("core", "utils"), g.sharedDependencies())
  }

  // ── transitiveDependentsOf ──────────────────────────────────────────────

  @Test
  fun `transitiveDependentsOf walks full chain`() {
    val g = graph(
      setOf("lib", "api", "app"),
      mapOf("api" to setOf("lib"), "app" to setOf("api")),
    )

    val dependents = g.transitiveDependentsOf("lib")
    assertEquals(setOf("api", "app"), dependents)
  }

  // ── topologicalTiers ────────────────────────────────────────────────────

  @Test
  fun `single project returns one tier`() {
    val g = graph(setOf("a"))

    val tiers = g.topologicalTiers()
    assertEquals(1, tiers.size)
    assertEquals(setOf("a"), tiers[0])
  }

  @Test
  fun `independent projects are in the same tier`() {
    val g = graph(setOf("a", "b", "c"))

    val tiers = g.topologicalTiers()
    assertEquals(1, tiers.size)
    assertEquals(setOf("a", "b", "c"), tiers[0])
  }

  @Test
  fun `linear chain produces one project per tier`() {
    // app depends on api depends on lib
    val g = graph(
      setOf("lib", "api", "app"),
      mapOf("api" to setOf("lib"), "app" to setOf("api")),
    )

    val tiers = g.topologicalTiers()
    assertEquals(3, tiers.size)
    assertEquals(setOf("lib"), tiers[0])
    assertEquals(setOf("api"), tiers[1])
    assertEquals(setOf("app"), tiers[2])
  }

  @Test
  fun `diamond dependency produces correct tiers`() {
    // shared <- a, b <- app
    val g = graph(
      setOf("shared", "a", "b", "app"),
      mapOf(
        "a" to setOf("shared"),
        "b" to setOf("shared"),
        "app" to setOf("a", "b"),
      ),
    )

    val tiers = g.topologicalTiers()
    assertEquals(3, tiers.size)
    assertEquals(setOf("shared"), tiers[0])
    assertEquals(setOf("a", "b"), tiers[1])
    assertEquals(setOf("app"), tiers[2])
  }

  @Test
  fun `cyclic nodes are included in final tier`() {
    val g = graph(
      setOf("a", "b"),
      mapOf("a" to setOf("b"), "b" to setOf("a")),
    )

    val tiers = g.topologicalTiers()
    assertTrue(tiers.isNotEmpty())
    val allInTiers = tiers.flatten().toSet()
    assertEquals(setOf("a", "b"), allInTiers)
  }
}
