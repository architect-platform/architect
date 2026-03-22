package io.github.architectplatform.cli.graph

import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO

/**
 * Renders a [TaskPlanDTO] as an ASCII dependency tree rooted at the target task.
 */
class TaskPlanTreeRenderer {

  fun render(plan: TaskPlanDTO): String {
    val sb = StringBuilder()
    sb.appendLine()
    sb.appendLine("━".repeat(80))
    sb.appendLine("🌳 Dependency Tree: ${plan.task}")
    sb.appendLine("📦 Project: ${plan.project}")
    sb.appendLine("━".repeat(80))
    sb.appendLine()
    sb.appendLine("  ${plan.totalSteps} task(s) across ${plan.parallelBatches} parallel batch(es)")
    sb.appendLine()

    val stepById = plan.steps.associateBy { it.id }
    // Find the root(s): the target task, or if not present as a step, find steps that nothing depends on
    val rootId = plan.task
    val rootStep = stepById[rootId]

    if (rootStep != null) {
      renderNode(sb, rootStep, stepById, prefix = "  ", isLast = true, isRoot = true)
    } else {
      // Fallback: find steps that are not depended on by any other step
      val dependedOn = plan.steps.flatMap { it.depends }.toSet()
      val roots = plan.steps.filter { it.id !in dependedOn }
      if (roots.isEmpty() && plan.steps.isNotEmpty()) {
        // all steps form a cycle or single nodes — just render all
        plan.steps.forEachIndexed { i, step ->
          renderNode(sb, step, stepById, prefix = "  ", isLast = i == plan.steps.size - 1, isRoot = true)
        }
      } else {
        roots.forEachIndexed { i, step ->
          renderNode(sb, step, stepById, prefix = "  ", isLast = i == roots.size - 1, isRoot = true)
        }
      }
    }

    sb.appendLine()
    return sb.toString()
  }

  private fun renderNode(
    sb: StringBuilder,
    step: TaskPlanStepDTO,
    stepById: Map<String, TaskPlanStepDTO>,
    prefix: String,
    isLast: Boolean,
    isRoot: Boolean,
    visited: MutableSet<String> = mutableSetOf(),
  ) {
    val connector = if (isRoot) "" else if (isLast) "└── " else "├── "
    val phase = if (step.phase != null) " [${step.phase}]" else ""
    val batch = " (batch ${step.batch})"
    sb.appendLine("$prefix$connector${step.id}$phase — ${step.description}$batch")

    if (step.id in visited) return
    visited.add(step.id)

    val children = step.depends.mapNotNull { stepById[it] }
    val childPrefix = if (isRoot) prefix else prefix + if (isLast) "    " else "│   "
    children.forEachIndexed { i, child ->
      renderNode(sb, child, stepById, childPrefix, isLast = i == children.size - 1, isRoot = false, visited = visited)
    }
  }
}
