package io.github.architectplatform.cli.graph

import io.github.architectplatform.engine.core.project.domain.ProjectDependencyGraph

class ProjectGraphDotRenderer {
  fun render(graphName: String, graph: ProjectDependencyGraph): String {
    val projects = graph.projects.sorted()
    val edges = graph.dependencies
      .flatMap { (project, dependencies) -> dependencies.map { dependency -> dependency to project } }
      .distinct()
      .sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })

    return buildString {
      appendLine("digraph \"${escape(graphName)}\" {")
      appendLine("  rankdir=LR;")
      appendLine("  node [shape=box, style=rounded];")
      appendLine()
      projects.forEach { project ->
        appendLine("  \"${escape(project)}\" [label=\"${escape(project)}\"];")
      }
      if (edges.isNotEmpty()) {
        appendLine()
        edges.forEach { (dependency, project) ->
          appendLine("  \"${escape(dependency)}\" -> \"${escape(project)}\";")
        }
      }
      appendLine("}")
    }
  }

  private fun escape(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
}