package io.github.architectplatform.api.core.tasks

/**
 * Represents the result of a task execution.
 *
 * TaskResult encapsulates the outcome of a task's execution, including whether it succeeded,
 * an optional message, and any sub-results from nested task executions.
 *
 * Example usage:
 * ```kotlin
 * // Success result
 * TaskResult.success("Build completed successfully")
 *
 * // Failure result
 * TaskResult.failure("Build failed: compilation error")
 *
 * // Skipped result (task decided not to execute)
 * TaskResult.skipped("No changes detected since last build")
 *
 * // Result with sub-results
 * TaskResult.success("All tests passed", listOf(
 *   TaskResult.success("Unit tests: 10 passed"),
 *   TaskResult.success("Integration tests: 5 passed")
 * ))
 * ```
 */
interface TaskResult {
  /**
   * Indicates whether the task execution was successful.
   * Skipped tasks are considered successful (they did not fail).
   */
  val success: Boolean

  /**
   * Optional message providing additional information about the execution result.
   */
  val message: String?

  /**
   * List of sub-results from nested task executions or subtasks.
   */
  val results: List<TaskResult>

  /**
   * Optional metadata about the task execution (timing, exit code, executor info).
   * Returns null by default for backward compatibility.
   */
  val metadata: TaskMetadata?
    get() = null

  /**
   * Arbitrary key-value data produced by this task for consumption by downstream tasks.
   *
   * Tasks can populate this map to pass structured data to dependent tasks.
   * The engine propagates this data through the dependency chain, making it
   * available via [TaskContext.upstreamData].
   *
   * Example usage:
   * ```kotlin
   * TaskResult.success(
   *   message = "Build completed",
   *   data = mapOf("artifactPath" to "/build/output.jar", "version" to "1.2.3")
   * )
   * ```
   */
  val data: Map<String, Any>
    get() = emptyMap()

  /**
   * The execution status of the task, providing finer granularity than [success].
   *
   * - [Status.SUCCESS]: Task completed successfully
   * - [Status.FAILURE]: Task failed
   * - [Status.SKIPPED]: Task was skipped (no work performed, not a failure)
   *
   * Defaults to [Status.SUCCESS] or [Status.FAILURE] based on [success] for backward compatibility.
   */
  val status: Status
    get() = if (success) Status.SUCCESS else Status.FAILURE

  /**
   * Represents the execution status of a task.
   */
  enum class Status {
    /** Task completed successfully. */
    SUCCESS,

    /** Task failed during execution. */
    FAILURE,

    /** Task was skipped (e.g., no changes detected, condition not met). */
    SKIPPED,

    /** Task completed with warnings (non-fatal issues detected). */
    WARNING,
  }

  companion object {
    /**
     * Internal implementation of TaskResult.
     */
    data class TaskResultImpl(
      override val success: Boolean,
      override val message: String? = null,
      override val results: List<TaskResult> = emptyList(),
      override val metadata: TaskMetadata? = null,
      override val data: Map<String, Any> = emptyMap(),
      override val status: Status = if (success) Status.SUCCESS else Status.FAILURE,
    ) : TaskResult

    /**
     * Creates a successful task result.
     *
     * @param message Optional success message
     * @param results Optional list of sub-results
     * @param metadata Optional execution metadata
     * @param data Optional key-value data for downstream tasks
     * @return A successful TaskResult
     */
    fun success(
      message: String? = null,
      results: List<TaskResult> = emptyList(),
      metadata: TaskMetadata? = null,
      data: Map<String, Any> = emptyMap(),
    ): TaskResult =
      TaskResultImpl(
        success = true,
        message = message,
        results = results,
        metadata = metadata,
        data = data,
        status = Status.SUCCESS,
      )

    /**
     * Creates a failed task result.
     *
     * @param message Optional failure message describing what went wrong
     * @param results Optional list of sub-results
     * @param metadata Optional execution metadata
     * @param data Optional key-value data for downstream tasks
     * @return A failed TaskResult
     */
    fun failure(
      message: String? = null,
      results: List<TaskResult> = emptyList(),
      metadata: TaskMetadata? = null,
      data: Map<String, Any> = emptyMap(),
    ): TaskResult =
      TaskResultImpl(
        success = false,
        message = message,
        results = results,
        metadata = metadata,
        data = data,
        status = Status.FAILURE,
      )

    /**
     * Creates a skipped task result, indicating the task chose not to execute.
     *
     * Skipped tasks are considered successful ([success] = true) but can be
     * distinguished from actual executions via [status] = [Status.SKIPPED].
     * The engine renders skipped tasks differently in output.
     *
     * @param reason Explanation of why the task was skipped
     * @return A skipped TaskResult
     */
    fun skipped(reason: String): TaskResult =
      TaskResultImpl(
        success = true,
        message = reason,
        status = Status.SKIPPED,
      )

    /**
     * Creates a warning task result for non-fatal issues.
     *
     * Warning results are considered successful ([success] = true) but signal
     * that something noteworthy occurred. The CLI renders warnings with a ⚠️ icon
     * in yellow to draw attention without blocking execution.
     *
     * @param message Description of the warning condition
     * @param results Optional list of sub-results
     * @param metadata Optional execution metadata
     * @param data Optional key-value data for downstream tasks
     * @return A warning TaskResult
     */
    fun warning(
      message: String,
      results: List<TaskResult> = emptyList(),
      metadata: TaskMetadata? = null,
      data: Map<String, Any> = emptyMap(),
    ): TaskResult =
      TaskResultImpl(
        success = true,
        message = message,
        results = results,
        metadata = metadata,
        data = data,
        status = Status.WARNING,
      )
  }
}
