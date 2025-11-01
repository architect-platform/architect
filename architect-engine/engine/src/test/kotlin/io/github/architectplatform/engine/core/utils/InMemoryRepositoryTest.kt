package io.github.architectplatform.engine.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for InMemoryRepository.
 */
class InMemoryRepositoryTest {

    private lateinit var repository: TestRepository

    @BeforeEach
    fun setup() {
        repository = TestRepository()
    }

    @Test
    fun `should save and retrieve object by key`() {
        // Given
        val key = "test-key"
        val value = "test-value"

        // When
        repository.save(key, value)
        val retrieved = repository.get(key)

        // Then
        assertEquals(value, retrieved)
    }

    @Test
    fun `should return null for non-existent key`() {
        // When
        val retrieved = repository.get("non-existent")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `should overwrite existing value with same key`() {
        // Given
        val key = "test-key"
        val value1 = "value1"
        val value2 = "value2"

        // When
        repository.save(key, value1)
        repository.save(key, value2)
        val retrieved = repository.get(key)

        // Then
        assertEquals(value2, retrieved)
    }

    @Test
    fun `should retrieve all saved objects`() {
        // Given
        repository.save("key1", "value1")
        repository.save("key2", "value2")
        repository.save("key3", "value3")

        // When
        val all = repository.getAll()

        // Then
        assertEquals(3, all.size)
        assertTrue(all.contains("value1"))
        assertTrue(all.contains("value2"))
        assertTrue(all.contains("value3"))
    }

    @Test
    fun `should return empty list when no objects saved`() {
        // When
        val all = repository.getAll()

        // Then
        assertTrue(all.isEmpty())
    }

    @Test
    fun `should maintain unique keys in getAll`() {
        // Given
        val key = "test-key"
        repository.save(key, "value1")
        repository.save(key, "value2") // Overwrite

        // When
        val all = repository.getAll()

        // Then
        assertEquals(1, all.size)
        assertEquals("value2", all[0])
    }

    /**
     * Test implementation of InMemoryRepository.
     */
    class TestRepository : InMemoryRepository<String>()
}
