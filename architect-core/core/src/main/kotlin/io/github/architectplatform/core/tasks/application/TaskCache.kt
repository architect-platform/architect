package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult
import java.util.concurrent.ConcurrentHashMap

class TaskCache(
  private val cacheEnabled: Boolean = false,
  private val ttlSeconds: Long = 0,
) {

  private data class CacheEntry(
    val result: TaskResult,
    val storedAtMillis: Long,
  )

  private val cache = ConcurrentHashMap<String, CacheEntry>()

  fun isCached(taskId: String): Boolean = get(taskId) != null

  fun get(taskId: String): TaskResult? {
    if (!cacheEnabled) {
      return null
    }
    val entry = cache[taskId] ?: return null
    if (isExpired(entry)) {
      cache.remove(taskId, entry)
      return null
    }
    return entry.result
  }

  fun store(taskId: String, result: TaskResult) {
    if (cacheEnabled) {
      cache[taskId] = CacheEntry(result, System.currentTimeMillis())
    }
  }

  fun clear() {
    cache.clear()
  }

  private fun isExpired(entry: CacheEntry): Boolean {
    if (ttlSeconds <= 0) {
      return false
    }
    val ageMillis = System.currentTimeMillis() - entry.storedAtMillis
    return ageMillis >= ttlSeconds * 1000
  }
}
