package io.github.architectplatform.cli.graph

import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO

class TaskGraphDotRenderer {
  fun render(graphName: String, plans: List<TaskPlanDTO>): String =
    renderSteps(graphName, plans.flatMap { it.steps })

  fun renderSteps(graphName: String, steps: List<TaskPlanStepDTO>): String {
    val nodes = linkedMapOf<String, TaskPlanStepDTO>()
    steps.sortedBy { it.id }.forEach { step -> nodes.putIfAbsent(step.id, step) }
    steps.flatMap { it.depends }.sorted().forEach { dependencyId ->
      nodes.putIfAbsent(
        dependencyId,
        TaskPlanStepDTO(id = dependencyId, description = "", phase = null, depends = emptyList(), batch = 0),
      )
    }

    val edges = steps
      .flatMap { step -> step.depends.map { dependencyId -> dependencyId to step.id } }
      .distinct()
      .sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })

    return buildString {
      appendLine("digraph \"${escape(graphName)}\" {")
      appendLine("  rankdir=LR;")
      appendLine("  node [shape=box, style=rounded];")
      appendLine()
      nodes.forEach { (id, step) ->
        appendLine("  \"${escape(id)}\" [label=\"${nodeLabel(step)}\"];")
      }
      if (edges.isNotEmpty()) {
        appendLine()
        edges.forEach { (dependencyId, taskId) ->
          appendLine("  \"${escape(dependencyId)}\" -> \"${escape(taskId)}\";")
        }
      }
      appendLine("}")
    }
  }

  private fun nodeLabel(step: TaskPlanStepDTO): String {
    val parts = listOfNotNull(
      step.id,
      step.phase?.takeIf { it.isNotBlank() }?.let { "[$it]" },
      step.description.takeIf { it.isNotBlank() },
    )
    return escape(parts.joinToString("\\n"))
  }

  private fun escape(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
}