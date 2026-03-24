package io.github.architectplatform.plugins.scripts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ShellCommandSanitizerTest {

    @Test
    fun `escapeShellArg should wrap simple strings in single quotes`() {
        val result = ShellCommandSanitizer.escapeShellArg("hello")
        assertEquals("'hello'", result)
    }

    @Test
    fun `escapeShellArg should escape single quotes`() {
        val result = ShellCommandSanitizer.escapeShellArg("it's")
        assertEquals("'it'\\''s'", result)
    }

    @Test
    fun `escapeShellArg should handle multiple single quotes`() {
        val result = ShellCommandSanitizer.escapeShellArg("don't can't won't")
        assertEquals("'don'\\''t can'\\''t won'\\''t'", result)
    }

    @Test
    fun `escapeShellArg should handle empty string`() {
        val result = ShellCommandSanitizer.escapeShellArg("")
        assertEquals("''", result)
    }

    @Test
    fun `escapeShellArg should handle special characters`() {
        val result = ShellCommandSanitizer.escapeShellArg("test; rm -rf /")
        assertEquals("'test; rm -rf /'", result)
    }

    @Test
    fun `escapeShellArg should handle dollar signs and backticks`() {
        val result = ShellCommandSanitizer.escapeShellArg("\$HOME `whoami`")
        assertEquals("'\$HOME `whoami`'", result)
    }

    @Test
    fun `validateEnvKey should accept valid uppercase keys`() {
        val result = ShellCommandSanitizer.validateEnvKey("MY_VAR")
        assertEquals("MY_VAR", result)
    }

    @Test
    fun `validateEnvKey should accept keys with numbers`() {
        val result = ShellCommandSanitizer.validateEnvKey("VAR_123")
        assertEquals("VAR_123", result)
    }

    @Test
    fun `validateEnvKey should accept keys starting with underscore`() {
        val result = ShellCommandSanitizer.validateEnvKey("_PRIVATE_VAR")
        assertEquals("_PRIVATE_VAR", result)
    }

    @Test
    fun `validateEnvKey should reject lowercase keys`() {
        assertThrows<IllegalArgumentException> {
            ShellCommandSanitizer.validateEnvKey("my_var")
        }
    }

    @Test
    fun `validateEnvKey should reject keys with special characters`() {
        assertThrows<IllegalArgumentException> {
            ShellCommandSanitizer.validateEnvKey("MY-VAR")
        }
    }

    @Test
    fun `validateEnvKey should reject keys starting with numbers`() {
        assertThrows<IllegalArgumentException> {
            ShellCommandSanitizer.validateEnvKey("123VAR")
        }
    }

    @Test
    fun `validateEnvKey should reject empty keys`() {
        assertThrows<IllegalArgumentException> {
            ShellCommandSanitizer.validateEnvKey("")
        }
    }

    @Test
    fun `escapeEnvValue should wrap values in double quotes`() {
        val result = ShellCommandSanitizer.escapeEnvValue("hello")
        assertEquals("\"hello\"", result)
    }

    @Test
    fun `escapeEnvValue should escape double quotes`() {
        val result = ShellCommandSanitizer.escapeEnvValue("say \"hello\"")
        assertEquals("\"say \\\"hello\\\"\"", result)
    }

    @Test
    fun `escapeEnvValue should escape dollar signs`() {
        val result = ShellCommandSanitizer.escapeEnvValue("\$HOME")
        assertEquals("\"\\\$HOME\"", result)
    }

    @Test
    fun `escapeEnvValue should escape backticks`() {
        val result = ShellCommandSanitizer.escapeEnvValue("`whoami`")
        assertEquals("\"\\`whoami\\`\"", result)
    }

    @Test
    fun `escapeEnvValue should escape backslashes`() {
        val result = ShellCommandSanitizer.escapeEnvValue("C:\\Users\\test")
        assertEquals("\"C:\\\\Users\\\\test\"", result)
    }

    @Test
    fun `escapeEnvValue should escape exclamation marks`() {
        val result = ShellCommandSanitizer.escapeEnvValue("Hello!")
        assertEquals("\"Hello\\!\"", result)
    }

    @Test
    fun `escapeEnvValue should handle empty string`() {
        val result = ShellCommandSanitizer.escapeEnvValue("")
        assertEquals("\"\"", result)
    }

    @Test
    fun `escapeEnvValue should handle complex values`() {
        val result = ShellCommandSanitizer.escapeEnvValue("path=\"/usr/bin\" && echo \$PATH")
        assertEquals("\"path=\\\"/usr/bin\\\" && echo \\\$PATH\"", result)
    }

    @Test
    fun `validateCommand should always return true`() {
        // This is a placeholder validation that allows all commands
        assertTrue(ShellCommandSanitizer.validateCommand("echo hello"))
        assertTrue(ShellCommandSanitizer.validateCommand("rm -rf /"))
        assertTrue(ShellCommandSanitizer.validateCommand("echo hello; whoami"))
    }
}
