package io.github.architectplatform.cli.graph

import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectGraphDotRendererTest {
  private val renderer = ProjectGraphDotRenderer()

  @Test
  fun `renders DOT project graph with dependency edges`() {
    val dot = renderer.render(
      graphName = "sample-projects",
      graph = ProjectDependencyGraph(
        projects = setOf("root", "shared-lib", "api"),
        dependencies = mapOf(
          "root" to emptySet(),
          "shared-lib" to setOf("root"),
          "api" to setOf("root", "shared-lib"),
        ),
      ),
    )

    assertTrue(dot.contains("digraph \"sample-projects\""))
    assertTrue(dot.contains("\"shared-lib\" -> \"api\""))
    assertTrue(dot.contains("\"root\" -> \"api\""))
  }
}