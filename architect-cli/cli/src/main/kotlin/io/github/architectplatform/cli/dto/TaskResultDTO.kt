package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Hierarchical result of task execution.
 *
 * Represents the outcome of a task, including nested sub-task results.
 * Provides a tree-like structure for representing complex execution flows.
 *
 * @property success Whether the task completed successfully
 * @property message Optional message describing the result
 * @property subResults Results of any sub-tasks executed as part of this task
 */
@Serdeable
data class TaskResultDTO(
    val success: Boolean,
    val message: String?,
    val subResults: List<TaskResultDTO> = emptyList(),
) {
  /**
   * Renders the task result as a tree structure with success/failure icons.
   */
  override fun toString(): String = render()

  /**
   * Recursively renders the task result tree.
   *
   * @param indent Current indentation level
   * @param isLast Whether this is the last item in its sibling list
   * @return Formatted string representation
   */
  private fun render(indent: String = "", isLast: Boolean = true): String {
    val branch = if (isLast) "└── " else "├── "
    val statusIcon = if (success) "✅" else "❌"
    val msg = message?.let { ": $it" } ?: ""

    val sb = StringBuilder()
    sb.append("$indent$branch$statusIcon$msg\n")

    subResults.forEachIndexed { index, sub ->
      val isSubLast = index == subResults.lastIndex
      sb.append(sub.render(indent + if (isLast) "    " else "│   ", isSubLast))
    }

    return sb.toString()
  }
}
