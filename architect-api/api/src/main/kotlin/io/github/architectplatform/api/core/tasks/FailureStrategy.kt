package io.github.architectplatform.api.core.tasks

/**
 * Strategy controlling how the engine handles task failures.
 *
 * Tasks declare their preferred strategy via [Task.onFailure]. The engine
 * evaluates it when a task returns [TaskResult] with `success == false`.
 */
sealed class FailureStrategy {

  /**
   * Stop execution immediately. This is the default behaviour.
   */
  data object ABORT : FailureStrategy()

  /**
   * Mark the task as failed but continue executing subsequent tasks.
   *
   * Useful for non-critical tasks (linting, optional checks) where a
   * failure should not block the rest of the pipeline.
   */
  data object CONTINUE : FailureStrategy()

  /**
   * Retry the task up to [maxAttempts] times before giving up.
   *
   * After exhausting retries the engine falls back to [ABORT].
   *
   * @param maxAttempts Maximum number of retry attempts (must be ≥ 1)
   */
  data class RETRY(val maxAttempts: Int = 1) : FailureStrategy() {
    init {
      require(maxAttempts >= 1) { "maxAttempts must be >= 1, got $maxAttempts" }
    }
  }
}
