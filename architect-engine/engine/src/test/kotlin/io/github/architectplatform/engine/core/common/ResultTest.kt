package io.github.architectplatform.engine.core.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for Result type.
 */
class ResultTest {

    @Test
    fun `should create success result`() {
        // When
        val result = Result.success("test value")

        // Then
        assertTrue(result.isSuccess())
        assertFalse(result.isFailure())
        assertEquals("test value", result.getOrNull())
    }

    @Test
    fun `should create failure result`() {
        // When
        val result = Result.failure<String>("error message")

        // Then
        assertFalse(result.isSuccess())
        assertTrue(result.isFailure())
        assertNull(result.getOrNull())
    }

    @Test
    fun `should get value or throw for success`() {
        // Given
        val result = Result.success("test")

        // When & Then
        assertEquals("test", result.getOrThrow())
    }

    @Test
    fun `should throw for failure when getting or throw`() {
        // Given
        val result = Result.failure<String>("error")

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            result.getOrThrow()
        }
    }

    @Test
    fun `should get value or default for success`() {
        // Given
        val result = Result.success("actual")

        // When
        val value = result.getOrElse("default")

        // Then
        assertEquals("actual", value)
    }

    @Test
    fun `should get default for failure`() {
        // Given
        val result = Result.failure<String>("error")

        // When
        val value = result.getOrElse("default")

        // Then
        assertEquals("default", value)
    }

    @Test
    fun `should map success value`() {
        // Given
        val result = Result.success(5)

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertTrue(mapped.isSuccess())
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `should not map failure value`() {
        // Given
        val result = Result.failure<Int>("error")

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertTrue(mapped.isFailure())
    }

    @Test
    fun `should flat map success value`() {
        // Given
        val result = Result.success(5)

        // When
        val mapped = result.flatMap { Result.success(it * 2) }

        // Then
        assertTrue(mapped.isSuccess())
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `should not flat map failure value`() {
        // Given
        val result = Result.failure<Int>("error")

        // When
        val mapped = result.flatMap { Result.success(it * 2) }

        // Then
        assertTrue(mapped.isFailure())
    }

    @Test
    fun `should execute onSuccess for success result`() {
        // Given
        var executed = false
        val result = Result.success("test")

        // When
        result.onSuccess { executed = true }

        // Then
        assertTrue(executed)
    }

    @Test
    fun `should not execute onSuccess for failure result`() {
        // Given
        var executed = false
        val result = Result.failure<String>("error")

        // When
        result.onSuccess { executed = true }

        // Then
        assertFalse(executed)
    }

    @Test
    fun `should execute onFailure for failure result`() {
        // Given
        var executed = false
        val result = Result.failure<String>("error")

        // When
        result.onFailure { executed = true }

        // Then
        assertTrue(executed)
    }

    @Test
    fun `should not execute onFailure for success result`() {
        // Given
        var executed = false
        val result = Result.success("test")

        // When
        result.onFailure { executed = true }

        // Then
        assertFalse(executed)
    }

    @Test
    fun `should fold success result`() {
        // Given
        val result = Result.success(5)

        // When
        val folded = result.fold(
            onSuccess = { "Success: $it" },
            onFailure = { "Failure: ${it.message}" }
        )

        // Then
        assertEquals("Success: 5", folded)
    }

    @Test
    fun `should fold failure result`() {
        // Given
        val result = Result.failure<Int>("error occurred")

        // When
        val folded = result.fold(
            onSuccess = { "Success: $it" },
            onFailure = { "Failure: ${it.message}" }
        )

        // Then
        assertEquals("Failure: error occurred", folded)
    }

    @Test
    fun `should catch successful operation`() {
        // When
        val result = Result.catching { "success" }

        // Then
        assertTrue(result.isSuccess())
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun `should catch failed operation`() {
        // When
        val result = Result.catching<String> {
            throw RuntimeException("error")
        }

        // Then
        assertTrue(result.isFailure())
    }

    @Test
    fun `should preserve exception in failure`() {
        // Given
        val exception = RuntimeException("test error")

        // When
        val result = Result.failure<String>("error", exception)

        // Then
        val failure = result as Result.Failure
        assertEquals("error", failure.message)
        assertSame(exception, failure.cause)
    }

    @Test
    fun `should support error code in failure`() {
        // When
        val result = Result.failure<String>("error", errorCode = "ERR_001")

        // Then
        val failure = result as Result.Failure
        assertEquals("ERR_001", failure.errorCode)
    }

    @Test
    fun `should chain multiple map operations`() {
        // Given
        val result = Result.success(2)

        // When
        val chained = result
            .map { it * 2 }
            .map { it + 3 }
            .map { it.toString() }

        // Then
        assertTrue(chained.isSuccess())
        assertEquals("7", chained.getOrNull())
    }

    @Test
    fun `should chain onSuccess and onFailure`() {
        // Given
        var successCount = 0
        var failureCount = 0
        val result = Result.success("test")

        // When
        result
            .onSuccess { successCount++ }
            .onFailure { failureCount++ }

        // Then
        assertEquals(1, successCount)
        assertEquals(0, failureCount)
    }
}
