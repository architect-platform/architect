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
   * @param maxAttempts   Maximum number of retry attempts (must be ≥ 1)
   * @param backoffMs     Base delay in milliseconds between retries (0 = no delay)
   * @param exponential   If true, doubles the delay with each attempt
   * @param jitter        If true, adds ±25 % random jitter to the computed delay
   */
  data class RETRY(
    val maxAttempts: Int = 1,
    val backoffMs: Long = 0L,
    val exponential: Boolean = false,
    val jitter: Boolean = false,
  ) : FailureStrategy() {
    init {
      require(maxAttempts >= 1) { "maxAttempts must be >= 1, got $maxAttempts" }
      require(backoffMs >= 0) { "backoffMs must be >= 0, got $backoffMs" }
    }

    /**
     * Computes the delay to wait before [attempt] (1-based, where 1 is the first retry).
     * Returns 0 if [backoffMs] is 0.
     */
    fun computeDelayMs(attempt: Int): Long {
      if (backoffMs == 0L) return 0L
      val base = if (exponential) backoffMs * (1L shl (attempt - 1).coerceAtMost(30)) else backoffMs
      if (!jitter) return base
      // ±25 % jitter
      val spread = (base * 0.25).toLong()
      return base + (Math.random() * spread * 2 - spread).toLong()
    }
  }
}
