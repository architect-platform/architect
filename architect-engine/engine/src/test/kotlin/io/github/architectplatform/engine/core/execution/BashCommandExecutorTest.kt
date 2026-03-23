package io.github.architectplatform.engine.core.execution

import io.github.architectplatform.api.core.tasks.TaskPermission
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
    fun `should enforce timeout`() {
        val timeoutExecutor = BashCommandExecutor(timeoutSeconds = 1)

        val exception = assertThrows(IllegalStateException::class.java) {
            timeoutExecutor.execute("sleep 2")
        }

        assertTrue(exception.message!!.contains("timed out after 1 seconds"))
    }

    @Test
    fun `should throw exception for non zero exit code`() {
        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            executor.execute("printf 'boom' >&2; exit 7")
        }
        assertTrue(exception.message!!.contains("Command failed with exit code 7"))
        assertTrue(exception.message!!.contains("boom"))
    }

    @Test
    fun `should execute command in working directory`(@TempDir tempDir: Path) {
        // Given
        val workingDirFile = File(tempDir.toFile(), "cwd.txt")
        
        // When
        executor.execute("pwd > cwd.txt", tempDir.toString())

        // Then
        assertTrue(workingDirFile.exists())
        assertEquals(File(tempDir.toString()).canonicalPath, File(workingDirFile.readText().trim()).canonicalPath)
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
    fun `should execute command with environment variables`(@TempDir tempDir: Path) {
        val outputFile = File(tempDir.toFile(), "env.txt")

        executor.execute("TEST_VAR=injected-value sh -c 'printf %s \"\$TEST_VAR\" > env.txt'", tempDir.toString())

        assertTrue(outputFile.exists())
        assertEquals("injected-value", outputFile.readText())
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

    @Test
    fun `should reject process execution when task lacks permission`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            TaskPermissionScope.withPermissions("forbidden-task", setOf(TaskPermission.FILE_SYSTEM_READ)) {
                executor.execute("echo 'blocked'")
            }
        }

        assertTrue(exception.message!!.contains("process:exec"))
    }
}
