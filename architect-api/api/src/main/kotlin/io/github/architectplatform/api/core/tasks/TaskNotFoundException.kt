package io.github.architectplatform.api.core.tasks

/**
 * Thrown when a task is requested but does not exist in the registry.
 *
 * Includes the requested task name, available task IDs, and suggests
 * similar names based on edit distance.
 */
class TaskNotFoundException(
  val taskId: String,
  val projectName: String,
  availableTaskIds: List<String> = emptyList(),
) : IllegalArgumentException(buildMessage(taskId, projectName, availableTaskIds)) {
  companion object {
    private fun buildMessage(taskId: String, projectName: String, available: List<String>): String {
      val base = "Task '$taskId' not found in project '$projectName'."
      if (available.isEmpty()) {
        return "$base No tasks are registered."
      }
      val suggestions = findSimilar(taskId, available, maxSuggestions = 3)
      return if (suggestions.isNotEmpty()) {
        "$base Did you mean: ${suggestions.joinToString(", ") { "'$it'" }}?"
      } else {
        "$base Available tasks: ${available.sorted().joinToString(", ") { "'$it'" }}."
      }
    }

    internal fun findSimilar(target: String, candidates: List<String>, maxSuggestions: Int = 3): List<String> {
      return candidates
        .map { it to levenshtein(target.lowercase(), it.lowercase()) }
        .filter { it.second <= maxOf(3, target.length / 2) }
        .sortedBy { it.second }
        .take(maxSuggestions)
        .map { it.first }
    }

    internal fun levenshtein(a: String, b: String): Int {
      val m = a.length
      val n = b.length
      val dp = Array(m + 1) { IntArray(n + 1) }
      for (i in 0..m) dp[i][0] = i
      for (j in 0..n) dp[0][j] = j
      for (i in 1..m) {
        for (j in 1..n) {
          dp[i][j] =
            if (a[i - 1] == b[j - 1]) {
              dp[i - 1][j - 1]
            } else {
              1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
      }
      return dp[m][n]
    }
  }
}
