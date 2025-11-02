package io.github.architectplatform.plugins.scripts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ScriptUtilsTest {

    @Test
    fun `escapeShellArg should wrap simple strings in single quotes`() {
        val result = ScriptUtils.escapeShellArg("hello")
        assertEquals("'hello'", result)
    }

    @Test
    fun `escapeShellArg should escape single quotes`() {
        val result = ScriptUtils.escapeShellArg("it's")
        assertEquals("'it'\\''s'", result)
    }

    @Test
    fun `escapeShellArg should handle multiple single quotes`() {
        val result = ScriptUtils.escapeShellArg("don't can't won't")
        assertEquals("'don'\\''t can'\\''t won'\\''t'", result)
    }

    @Test
    fun `escapeShellArg should handle empty string`() {
        val result = ScriptUtils.escapeShellArg("")
        assertEquals("''", result)
    }

    @Test
    fun `escapeShellArg should handle special characters`() {
        val result = ScriptUtils.escapeShellArg("test; rm -rf /")
        assertEquals("'test; rm -rf /'", result)
    }

    @Test
    fun `escapeShellArg should handle dollar signs and backticks`() {
        val result = ScriptUtils.escapeShellArg("\$HOME `whoami`")
        assertEquals("'\$HOME `whoami`'", result)
    }

    @Test
    fun `validateEnvKey should accept valid uppercase keys`() {
        val result = ScriptUtils.validateEnvKey("MY_VAR")
        assertEquals("MY_VAR", result)
    }

    @Test
    fun `validateEnvKey should accept keys with numbers`() {
        val result = ScriptUtils.validateEnvKey("VAR_123")
        assertEquals("VAR_123", result)
    }

    @Test
    fun `validateEnvKey should accept keys starting with underscore`() {
        val result = ScriptUtils.validateEnvKey("_PRIVATE_VAR")
        assertEquals("_PRIVATE_VAR", result)
    }

    @Test
    fun `validateEnvKey should reject lowercase keys`() {
        assertThrows<IllegalArgumentException> {
            ScriptUtils.validateEnvKey("my_var")
        }
    }

    @Test
    fun `validateEnvKey should reject keys with special characters`() {
        assertThrows<IllegalArgumentException> {
            ScriptUtils.validateEnvKey("MY-VAR")
        }
    }

    @Test
    fun `validateEnvKey should reject keys starting with numbers`() {
        assertThrows<IllegalArgumentException> {
            ScriptUtils.validateEnvKey("123VAR")
        }
    }

    @Test
    fun `validateEnvKey should reject empty keys`() {
        assertThrows<IllegalArgumentException> {
            ScriptUtils.validateEnvKey("")
        }
    }

    @Test
    fun `escapeEnvValue should wrap values in double quotes`() {
        val result = ScriptUtils.escapeEnvValue("hello")
        assertEquals("\"hello\"", result)
    }

    @Test
    fun `escapeEnvValue should escape double quotes`() {
        val result = ScriptUtils.escapeEnvValue("say \"hello\"")
        assertEquals("\"say \\\"hello\\\"\"", result)
    }

    @Test
    fun `escapeEnvValue should escape dollar signs`() {
        val result = ScriptUtils.escapeEnvValue("\$HOME")
        assertEquals("\"\\\$HOME\"", result)
    }

    @Test
    fun `escapeEnvValue should escape backticks`() {
        val result = ScriptUtils.escapeEnvValue("`whoami`")
        assertEquals("\"\\`whoami\\`\"", result)
    }

    @Test
    fun `escapeEnvValue should escape backslashes`() {
        val result = ScriptUtils.escapeEnvValue("C:\\Users\\test")
        assertEquals("\"C:\\\\Users\\\\test\"", result)
    }

    @Test
    fun `escapeEnvValue should escape exclamation marks`() {
        val result = ScriptUtils.escapeEnvValue("Hello!")
        assertEquals("\"Hello\\!\"", result)
    }

    @Test
    fun `escapeEnvValue should handle empty string`() {
        val result = ScriptUtils.escapeEnvValue("")
        assertEquals("\"\"", result)
    }

    @Test
    fun `escapeEnvValue should handle complex values`() {
        val result = ScriptUtils.escapeEnvValue("path=\"/usr/bin\" && echo \$PATH")
        assertEquals("\"path=\\\"/usr/bin\\\" && echo \\\$PATH\"", result)
    }

    @Test
    fun `validateCommand should always return true`() {
        // This is a placeholder validation that allows all commands
        assertTrue(ScriptUtils.validateCommand("echo hello"))
        assertTrue(ScriptUtils.validateCommand("rm -rf /"))
        assertTrue(ScriptUtils.validateCommand("echo hello; whoami"))
    }
}
