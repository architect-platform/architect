package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class TaskCache(
  @Property(name = EngineConfiguration.Cache.ENABLED, defaultValue = "${EngineConfiguration.Cache.DEFAULT_ENABLED}")
  private val cacheEnabled: Boolean = false,
  @Property(name = EngineConfiguration.Cache.TTL_SECONDS, defaultValue = "${EngineConfiguration.Cache.DEFAULT_TTL_SECONDS}")
  private val ttlSeconds: Long = EngineConfiguration.Cache.DEFAULT_TTL_SECONDS,
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
