package io.github.architectplatform.cli.embedded

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MultiProjectOrchestratorTest {

  private fun graph(vararg pairs: Pair<String, Set<String>>): ProjectDependencyGraph =
    ProjectDependencyGraph(projects = pairs.map { it.first }.toSet(), dependencies = pairs.toMap())

  private fun stubRunner(results: Map<String, TaskResult>): MultiProjectOrchestrator.TaskRunner =
    MultiProjectOrchestrator.TaskRunner { name, _, _, _, _ ->
      results[name] ?: TaskResult.failure("unknown project: $name")
    }

  private fun orchestratorWithRunner(
    results: Map<String, TaskResult>,
  ): Pair<MultiProjectOrchestrator, MultiProjectOrchestrator.TaskRunner> {
    val executor = ApplicationContext.run().getBean(EmbeddedTaskExecutor::class.java)
    return MultiProjectOrchestrator(executor) to stubRunner(results)
  }

  @Test
  fun `should execute single project and return success`() {
    val g = graph("app" to emptySet())
    val (orchestrator, runner) = orchestratorWithRunner(mapOf("app" to TaskResult.success("OK")))

    val result = orchestrator.run(g, mapOf("app" to "/app"), "build", runner = runner)

    assertTrue(result.success)
    assertEquals(1, result.results.size)
    assertEquals("app", result.results[0].projectName)
    assertTrue(result.skipped.isEmpty())
  }

  @Test
  fun `should execute projects in dependency order across tiers`() {
    val g = graph("lib" to emptySet(), "app" to setOf("lib"))
    val order = mutableListOf<String>()
    val executor = ApplicationContext.run().getBean(EmbeddedTaskExecutor::class.java)
    val orchestrator = MultiProjectOrchestrator(executor)
    val runner = MultiProjectOrchestrator.TaskRunner { name, _, _, _, _ ->
      order += name
      TaskResult.success("OK")
    }

    orchestrator.run(g, mapOf("lib" to "/lib", "app" to "/app"), "build", runner = runner)

    assertTrue(order.indexOf("lib") < order.indexOf("app"))
  }

  @Test
  fun `should stop after failed tier when stopOnFailure is true`() {
    val g = graph("lib" to emptySet(), "app" to setOf("lib"))
    val (orchestrator, runner) = orchestratorWithRunner(mapOf("lib" to TaskResult.failure("compile error")))

    val result = orchestrator.run(g, mapOf("lib" to "/lib", "app" to "/app"), "build", stopOnFailure = true, runner = runner)

    assertFalse(result.success)
    assertEquals(1, result.results.size)
    assertEquals("lib", result.results[0].projectName)
    assertTrue(result.skipped.contains("app"))
  }

  @Test
  fun `should continue executing when stopOnFailure is false`() {
    val g = graph("lib" to emptySet(), "app" to setOf("lib"))
    val (orchestrator, runner) = orchestratorWithRunner(
      mapOf("lib" to TaskResult.failure("error"), "app" to TaskResult.success("OK")),
    )

    val result = orchestrator.run(g, mapOf("lib" to "/lib", "app" to "/app"), "build", stopOnFailure = false, runner = runner)

    assertFalse(result.success)
    assertEquals(2, result.results.size)
    assertTrue(result.skipped.isEmpty())
  }

  @Test
  fun `should only run target projects and skip others in graph`() {
    val g = graph("lib" to emptySet(), "app" to setOf("lib"), "tools" to emptySet())
    val (orchestrator, runner) = orchestratorWithRunner(
      mapOf("lib" to TaskResult.success("OK"), "app" to TaskResult.success("OK")),
    )

    val result = orchestrator.run(g, mapOf("lib" to "/lib", "app" to "/app"), "build", runner = runner)

    assertTrue(result.success)
    assertEquals(2, result.results.size)
    val names = result.results.map { it.projectName }
    assertTrue(names.contains("lib"))
    assertTrue(names.contains("app"))
    assertFalse(names.contains("tools"))
  }

  @Test
  fun `should report correct totalProjects including skipped`() {
    val g = graph("a" to emptySet(), "b" to setOf("a"), "c" to setOf("b"))
    val (orchestrator, runner) = orchestratorWithRunner(mapOf("a" to TaskResult.failure("fail")))

    val result = orchestrator.run(g, mapOf("a" to "/a", "b" to "/b", "c" to "/c"), "test", stopOnFailure = true, runner = runner)

    assertEquals(3, result.totalProjects)
    assertEquals(1, result.results.size)
    assertEquals(2, result.skipped.size)
  }

  @Test
  fun `should return empty success when no target projects given`() {
    val g = graph("lib" to emptySet())
    val executor = ApplicationContext.run().getBean(EmbeddedTaskExecutor::class.java)
    val orchestrator = MultiProjectOrchestrator(executor)

    val result = orchestrator.run(g, emptyMap(), "build")

    assertTrue(result.success)
    assertEquals(0, result.results.size)
    assertTrue(result.skipped.isEmpty())
  }
}
