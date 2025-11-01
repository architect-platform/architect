package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class TaskResultDTO(
    val success: Boolean,
    val message: String?,
    val subResults: List<TaskResultDTO> = emptyList(),
) {
  override fun toString(): String = render()

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
