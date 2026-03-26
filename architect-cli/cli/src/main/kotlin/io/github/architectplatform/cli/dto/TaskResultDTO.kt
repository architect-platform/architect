package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class TaskMetadataDTO(
    val durationMs: Long?,
    val exitCode: Int?,
    val startedAt: String?,
    val finishedAt: String?,
    val executorInfo: String?,
)

/**
 * Hierarchical result of task execution.
 *
 * Represents the outcome of a task, including nested sub-task results.
 * Provides a tree-like structure for representing complex execution flows.
 *
 * @property success Whether the task completed successfully
 * @property message Optional message describing the result
 * @property subResults Results of any sub-tasks executed as part of this task
 * @property metadata Optional execution metadata (timing, exit code, executor info)
 * @property data Arbitrary key-value data produced by the task for downstream consumption
 */
@Serdeable
data class TaskResultDTO(
    val success: Boolean,
    val message: String?,
    val subResults: List<TaskResultDTO> = emptyList(),
    val metadata: TaskMetadataDTO? = null,
    val data: Map<String, Any> = emptyMap(),
    val status: String = if (success) "SUCCESS" else "FAILURE",
) {
  /**
   * Renders the task result as a tree structure with success/failure/skipped icons.
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
    val statusIcon = when (status) {
      "SKIPPED" -> "⏭️"
      "WARNING" -> "⚠️"
      else -> if (success) "✅" else "❌"
    }
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
