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
      TaskResultImpl(success = true, message = message, results = results, metadata = metadata, data = data)

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
      TaskResultImpl(success = false, message = message, results = results, metadata = metadata, data = data)
  }
}
