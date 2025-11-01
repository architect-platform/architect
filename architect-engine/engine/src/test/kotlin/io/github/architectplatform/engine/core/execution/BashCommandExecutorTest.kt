package io.github.architectplatform.engine.core.execution

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Unit tests for BashCommandExecutor.
 */
class BashCommandExecutorTest {

    private lateinit var executor: BashCommandExecutor

    @BeforeEach
    fun setup() {
        executor = BashCommandExecutor()
    }

    @Test
    fun `should execute simple command successfully`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("echo 'Hello World'")
        }
    }

    @Test
    fun `should throw exception for failed command`() {
        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            executor.execute("exit 1")
        }
        assertTrue(exception.message!!.contains("Command failed with exit code 1"))
    }

    @Test
    fun `should execute command in working directory`(@TempDir tempDir: Path) {
        // Given
        val testFile = File(tempDir.toFile(), "test.txt")
        
        // When
        executor.execute("touch test.txt", tempDir.toString())

        // Then
        assertTrue(testFile.exists())
    }

    @Test
    fun `should execute command with pipe`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("echo 'test' | grep 'test'")
        }
    }

    @Test
    fun `should execute multi-line command`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("""
                echo 'line1'
                echo 'line2'
                echo 'line3'
            """.trimIndent())
        }
    }

    @Test
    fun `should handle command with special characters`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("echo 'Hello \"World\" with \$special chars'")
        }
    }

    @Test
    fun `should fail for non-existent command`() {
        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            executor.execute("nonexistentcommand123")
        }
        assertTrue(exception.message!!.contains("Command failed"))
    }

    @Test
    fun `should execute command with environment variables`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("export TEST_VAR=value; echo \$TEST_VAR")
        }
    }

    @Test
    fun `should fail when working directory does not exist`() {
        // When & Then
        assertThrows(Exception::class.java) {
            executor.execute("echo 'test'", "/non/existent/directory")
        }
    }

    @Test
    fun `should execute command with redirection`(@TempDir tempDir: Path) {
        // Given
        val outputFile = File(tempDir.toFile(), "output.txt")
        
        // When
        executor.execute("echo 'test output' > output.txt", tempDir.toString())

        // Then
        assertTrue(outputFile.exists())
        assertEquals("test output", outputFile.readText().trim())
    }

    @Test
    fun `should handle command with multiple commands chained`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("echo 'first' && echo 'second' && echo 'third'")
        }
    }

    @Test
    fun `should fail on first failed command in chain`() {
        // When & Then
        assertThrows(IllegalStateException::class.java) {
            executor.execute("echo 'first' && exit 1 && echo 'third'")
        }
    }

    @Test
    fun `should execute command with conditional logic`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("if [ 1 -eq 1 ]; then echo 'true'; fi")
        }
    }

    @Test
    fun `should execute command with loops`() {
        // When & Then - should not throw exception
        assertDoesNotThrow {
            executor.execute("for i in 1 2 3; do echo \$i; done")
        }
    }
}
