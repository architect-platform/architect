package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.project.domain.ProjectDependencyGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AffectedProjectResolverTest {

  // ── helpers ────────────────────────────────────────────────────────────

  private fun project(
    name: String,
    path: String,
    config: Map<String, Any> = emptyMap(),
    subProjects: List<Project> = emptyList(),
  ): Project =
    Project(
      name = name,
      path = path,
      context = ProjectContext(java.nio.file.Path.of(path), config),
      plugins = emptyList(),
      subProjects = subProjects,
    )

  private fun graph(
    projects: Set<String>,
    dependencies: Map<String, Set<String>> = emptyMap(),
  ): ProjectDependencyGraph =
    ProjectDependencyGraph(
      projects = projects,
      dependencies = projects.associateWith { dependencies[it].orEmpty() },
    )

  /**
   * Creates a resolver with a fake command runner that returns the given changed files
   * (one per line) for any `git diff` command.
   */
  private fun resolverWith(changedFiles: List<String>): AffectedProjectResolver =
    AffectedProjectResolver(
      commandRunner = { _, _ -> 0 to changedFiles.joinToString("\n") },
    )

  // ── no changes ─────────────────────────────────────────────────────────

  @Test
  fun `no changes returns empty set`() {
    val root = project(name = "root", path = "/repo")
    val g = graph(setOf("root"))
    val resolver = resolverWith(emptyList())

    val affected = resolver.resolve(root, g)

    assertTrue(affected.isEmpty())
  }

  @Test
  fun `git diff failure returns empty set`() {
    val root = project(name = "root", path = "/repo")
    val g = graph(setOf("root"))
    val resolver = AffectedProjectResolver(commandRunner = { _, _ -> 1 to "" })

    val affected = resolver.resolve(root, g)

    assertTrue(affected.isEmpty())
  }

  // ── root-only change ──────────────────────────────────────────────────

  @Test
  fun `change in root only affects root`() {
    val child = project(name = "child", path = "/repo/child")
    val root = project(name = "root", path = "/repo", subProjects = listOf(child))
    val g = graph(setOf("root", "child"), mapOf("child" to setOf("root")))
    val resolver = resolverWith(listOf("README.md"))

    val affected = resolver.resolve(root, g)

    // root is affected; child depends on root → child is transitively affected
    assertTrue("root" in affected)
    assertTrue("child" in affected)
  }

  @Test
  fun `change in root without dependents only returns root`() {
    val child = project(name = "child", path = "/repo/child")
    val root = project(name = "root", path = "/repo", subProjects = listOf(child))
    // No declared dependencies — child does NOT depend on root in this graph
    val g = graph(setOf("root", "child"))
    val resolver = resolverWith(listOf("build.gradle.kts"))

    val affected = resolver.resolve(root, g)

    assertEquals(setOf("root"), affected)
  }

  // ── direct child change ───────────────────────────────────────────────

  @Test
  fun `change in child maps to child project`() {
    val child = project(name = "child", path = "/repo/child")
    val root = project(name = "root", path = "/repo", subProjects = listOf(child))
    val g = graph(setOf("root", "child"))
    val resolver = resolverWith(listOf("child/src/Main.kt"))

    val affected = resolver.resolve(root, g)

    assertEquals(setOf("child"), affected)
  }

  // ── transitive dependency chain ───────────────────────────────────────

  @Test
  fun `transitive dependency chain is fully expanded`() {
    // lib → api → app  (app depends on api, api depends on lib)
    val lib = project(name = "lib", path = "/repo/lib")
    val api = project(name = "api", path = "/repo/api")
    val app = project(name = "app", path = "/repo/app")
    val root = project(name = "root", path = "/repo", subProjects = listOf(lib, api, app))
    val g = graph(
      setOf("root", "lib", "api", "app"),
      mapOf(
        "api" to setOf("lib"),
        "app" to setOf("api"),
      ),
    )
    val resolver = resolverWith(listOf("lib/src/Util.kt"))

    val affected = resolver.resolve(root, g)

    // lib changed → api depends on lib → app depends on api
    assertTrue("lib" in affected)
    assertTrue("api" in affected)
    assertTrue("app" in affected)
  }

  @Test
  fun `diamond dependency is handled without duplication`() {
    // shared ← moduleA
    // shared ← moduleB
    // moduleA, moduleB ← app
    val shared = project(name = "shared", path = "/repo/shared")
    val moduleA = project(name = "moduleA", path = "/repo/moduleA")
    val moduleB = project(name = "moduleB", path = "/repo/moduleB")
    val app = project(name = "app", path = "/repo/app")
    val root = project(name = "root", path = "/repo", subProjects = listOf(shared, moduleA, moduleB, app))
    val g = graph(
      setOf("root", "shared", "moduleA", "moduleB", "app"),
      mapOf(
        "moduleA" to setOf("shared"),
        "moduleB" to setOf("shared"),
        "app" to setOf("moduleA", "moduleB"),
      ),
    )
    val resolver = resolverWith(listOf("shared/src/Common.kt"))

    val affected = resolver.resolve(root, g)

    assertEquals(setOf("shared", "moduleA", "moduleB", "app"), affected)
  }

  // ── always-include ────────────────────────────────────────────────────

  @Test
  fun `always-include adds projects even with no changes`() {
    val shared = project(name = "shared-lib", path = "/repo/shared-lib")
    val root = project(name = "root", path = "/repo", subProjects = listOf(shared))
    val g = graph(setOf("root", "shared-lib"))
    val resolver = resolverWith(emptyList())

    val affected = resolver.resolve(
      root, g,
      config = AffectedProjectResolver.AffectedConfig(alwaysInclude = setOf("shared-lib")),
    )

    assertEquals(setOf("shared-lib"), affected)
  }

  @Test
  fun `always-include ignores projects not in graph`() {
    val root = project(name = "root", path = "/repo")
    val g = graph(setOf("root"))
    val resolver = resolverWith(emptyList())

    val affected = resolver.resolve(
      root, g,
      config = AffectedProjectResolver.AffectedConfig(alwaysInclude = setOf("nonexistent")),
    )

    assertTrue(affected.isEmpty())
  }

  // ── never-include ─────────────────────────────────────────────────────

  @Test
  fun `never-include excludes affected projects`() {
    val docs = project(name = "docs", path = "/repo/docs")
    val root = project(name = "root", path = "/repo", subProjects = listOf(docs))
    val g = graph(setOf("root", "docs"))
    val resolver = resolverWith(listOf("docs/guide.md"))

    val affected = resolver.resolve(
      root, g,
      config = AffectedProjectResolver.AffectedConfig(neverInclude = setOf("docs")),
    )

    assertTrue("docs" !in affected)
  }

  @Test
  fun `never-include overrides always-include`() {
    val shared = project(name = "shared", path = "/repo/shared")
    val root = project(name = "root", path = "/repo", subProjects = listOf(shared))
    val g = graph(setOf("root", "shared"))
    val resolver = resolverWith(emptyList())

    val affected = resolver.resolve(
      root, g,
      config = AffectedProjectResolver.AffectedConfig(
        alwaysInclude = setOf("shared"),
        neverInclude = setOf("shared"),
      ),
    )

    assertTrue("shared" !in affected)
  }

  // ── parseConfig ───────────────────────────────────────────────────────

  @Test
  fun `parseConfig returns defaults for null`() {
    val config = AffectedProjectResolver.parseConfig(null)

    assertTrue(config.alwaysInclude.isEmpty())
    assertTrue(config.neverInclude.isEmpty())
  }

  @Test
  fun `parseConfig returns defaults for missing affected section`() {
    val config = AffectedProjectResolver.parseConfig(mapOf("other" to "value"))

    assertTrue(config.alwaysInclude.isEmpty())
    assertTrue(config.neverInclude.isEmpty())
  }

  @Test
  fun `parseConfig parses always-include and never-include`() {
    val config = AffectedProjectResolver.parseConfig(
      mapOf(
        "affected" to mapOf(
          "always-include" to listOf("shared-lib", "infra"),
          "never-include" to listOf("docs"),
        ),
      ),
    )

    assertEquals(setOf("shared-lib", "infra"), config.alwaysInclude)
    assertEquals(setOf("docs"), config.neverInclude)
  }

  // ── mapFilesToProjects ────────────────────────────────────────────────

  @Test
  fun `deepest project wins in longest-prefix matching`() {
    val nested = project(name = "nested", path = "/repo/services/api")
    val services = project(name = "services", path = "/repo/services")
    val root = project(name = "root", path = "/repo", subProjects = listOf(services, nested))

    val resolver = resolverWith(emptyList())
    val result = resolver.mapFilesToProjects(
      listOf("services/api/src/Main.kt"),
      listOf(root, services, nested),
      "/repo",
    )

    assertEquals(setOf("nested"), result)
  }

  @Test
  fun `file not under any subproject maps to root`() {
    val child = project(name = "child", path = "/repo/child")
    val root = project(name = "root", path = "/repo", subProjects = listOf(child))

    val resolver = resolverWith(emptyList())
    val result = resolver.mapFilesToProjects(
      listOf("ci/pipeline.yml"),
      listOf(root, child),
      "/repo",
    )

    assertEquals(setOf("root"), result)
  }

  // ── cacheValidator hook ───────────────────────────────────────────────

  @Test
  fun `cacheValidator default is identity`() {
    val resolver = resolverWith(emptyList())
    val input = setOf("a", "b", "c")

    assertEquals(input, resolver.cacheValidator(input))
  }
}
