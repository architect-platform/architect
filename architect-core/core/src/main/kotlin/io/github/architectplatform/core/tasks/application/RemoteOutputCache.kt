package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult

/**
 * Interface for a remote task output cache backed by an HTTP or cloud service.
 *
 * Implementations store and retrieve task results by their content-hash cache key.
 * The remote cache complements [LocalOutputCache] by enabling cache sharing
 * across CI agents and developer machines.
 */
interface RemoteOutputCache {

  /**
   * Fetches a cached task result by key.
   *
   * @param key The SHA-256 content-hash cache key
   * @return The cached result, or null if not found or on network error
   */
  fun fetchResult(key: String): CachedTaskResult?

  /**
   * Stores a task result in the remote cache.
   *
   * @param key The SHA-256 content-hash cache key
   * @param result The task result to cache
   * @param stdout Optional captured standard output
   */
  fun storeResult(key: String, result: TaskResult, stdout: String? = null)

  /**
   * Serializable representation of a cached task result for remote transport.
   */
  data class CachedTaskResult(
    val success: Boolean,
    val message: String?,
    val stdout: String?,
  ) {
    fun toTaskResult(): TaskResult =
      if (success) TaskResult.success(message) else TaskResult.failure(message)
  }
}
