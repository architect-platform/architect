package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `should handle concurrent access safely`() {
        // Given
        val threads = (1..10).map { index ->
            Thread {
                cache.store("task-$index", TaskResult.success("Result $index"))
                cache.get("task-$index")
                cache.isCached("task-$index")
            }
        }

        // When & Then - should not throw
        assertDoesNotThrow {
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }
    }

    @Test
    fun `should handle clear with concurrent access`() {
        // Given
        val storeThread = Thread {
            repeat(100) {
                cache.store("task-$it", TaskResult.success())
            }
        }
        val clearThread = Thread {
            repeat(10) {
                Thread.sleep(5)
                cache.clear()
            }
        }

        // When & Then - should not throw
        assertDoesNotThrow {
            storeThread.start()
            clearThread.start()
            storeThread.join()
            clearThread.join()
        }
    }
}
