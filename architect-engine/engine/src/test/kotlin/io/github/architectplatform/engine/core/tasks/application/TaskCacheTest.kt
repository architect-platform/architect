package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.tasks.application.TaskCache
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections

/**
 * Unit tests for TaskCache.
 */
class TaskCacheTest {

    private lateinit var cache: TaskCache

    @BeforeEach
    fun setup() {
        cache = TaskCache()
    }

    @Test
    fun `should return null when cache is disabled by default`() {
        // Given
        val taskId = "test-task"
        val result = TaskResult.success("Task completed")
        
        // When
        cache.store(taskId, result)
        val retrieved = cache.get(taskId)

        // Then - cache is disabled by default
        assertNull(retrieved)
    }

    @Test
    fun `should return false for isCached when cache disabled`() {
        // Given
        val taskId = "test-task"
        val result = TaskResult.success()
        cache.store(taskId, result)
        
        // When & Then
        assertFalse(cache.isCached(taskId))
    }

    @Test
    fun `should clear cache without errors`() {
        // When & Then - should not throw
        assertDoesNotThrow {
            cache.clear()
        }
    }

    @Test
    fun `should handle multiple stores when disabled`() {
        // Given
        val task1 = "test-task-1"
        val task2 = "test-task-2"
        
        // When
        cache.store(task1, TaskResult.success())
        cache.store(task2, TaskResult.success())

        // Then
        assertFalse(cache.isCached(task1))
        assertFalse(cache.isCached(task2))
    }

    @Test
    fun `should support concurrent reads of cached entries`() {
        val enabledCache = TaskCache(cacheEnabled = true)
        enabledCache.store("task-1", TaskResult.success("cached-result"))
        val messages = Collections.synchronizedList(mutableListOf<String?>())

        val threads = (1..10).map {
            Thread {
                repeat(25) {
                    messages += enabledCache.get("task-1")?.message
                }
            }
        }

        assertDoesNotThrow {
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        assertEquals(250, messages.size)
        assertTrue(messages.all { it == "cached-result" })
    }

    @Test
    fun `should invalidate cached entries when cleared`() {
        val enabledCache = TaskCache(cacheEnabled = true)
        enabledCache.store("task-1", TaskResult.success("cached"))

        enabledCache.clear()

        assertFalse(enabledCache.isCached("task-1"))
        assertNull(enabledCache.get("task-1"))
    }

    @Test
    fun `should expire cached entries after ttl`() {
        val enabledCache = TaskCache(cacheEnabled = true, ttlSeconds = 1)
        enabledCache.store("task-ttl", TaskResult.success("cached"))

        assertTrue(enabledCache.isCached("task-ttl"))

        Thread.sleep(1100)

        assertNull(enabledCache.get("task-ttl"))
        assertFalse(enabledCache.isCached("task-ttl"))
    }
}
