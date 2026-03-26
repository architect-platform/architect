package io.github.architectplatform.api.core.progress

/**
 * Reports progress for long-running tasks.
 *
 * Implementations route progress updates to the appropriate output channel:
 * CLI renders a progress bar, the engine streams via SSE, and JSON mode
 * emits structured progress events.
 *
 * Example usage:
 * ```kotlin
 * val progress = environment.progressReporter()
 * files.forEachIndexed { index, file ->
 *   progress.report(index + 1, files.size, "Processing ${file.name}")
 * }
 * ```
 */
interface ProgressReporter {

  /**
   * Reports progress of a long-running operation.
   *
   * @param current The current step number (1-based)
   * @param total The total number of steps (0 if indeterminate)
   * @param message A human-readable description of the current step
   */
  fun report(current: Int, total: Int, message: String)

  companion object {
    /**
     * A no-op reporter that silently discards all progress updates.
     */
    val NOOP: ProgressReporter = object : ProgressReporter {
      override fun report(current: Int, total: Int, message: String) {}
    }
  }
}
