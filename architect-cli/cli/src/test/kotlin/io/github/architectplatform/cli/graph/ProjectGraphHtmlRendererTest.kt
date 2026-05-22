package io.github.architectplatform.cli.graph

import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectGraphHtmlRendererTest {
  private val renderer = ProjectGraphHtmlRenderer()

  @Test
  fun `renders mermaid html project graph`() {
    val html = renderer.render(
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

    assertTrue(html.contains("<pre class=\"mermaid\">"))
    assertTrue(html.contains("flowchart LR"))
    assertTrue(html.contains("project_0") || html.contains("project_1"))
    assertTrue(html.contains("Project Graph"))
  }
}
