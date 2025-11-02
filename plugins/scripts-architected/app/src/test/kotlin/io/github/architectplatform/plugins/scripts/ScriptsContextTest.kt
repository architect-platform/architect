package io.github.architectplatform.plugins.scripts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScriptsContextTest {

    @Test
    fun `default context should be enabled with empty scripts`() {
        val context = ScriptsContext()

        assertTrue(context.enabled)
        assertTrue(context.scripts.isEmpty())
    }

    @Test
    fun `context should accept custom enabled state`() {
        val context = ScriptsContext(enabled = false)

        assertFalse(context.enabled)
    }

    @Test
    fun `context should accept scripts configuration`() {
        val scripts = mapOf(
            "hello" to ScriptConfig(
                command = "echo 'Hello'",
                description = "Prints hello"
            )
        )
        val context = ScriptsContext(scripts = scripts)

        assertEquals(1, context.scripts.size)
        assertTrue(context.scripts.containsKey("hello"))
    }

    @Test
    fun `script config should have default values`() {
        val config = ScriptConfig(command = "echo test")

        assertEquals("echo test", config.command)
        assertEquals("Custom script", config.description)
        assertNull(config.phase)
        assertEquals(".", config.workingDirectory)
        assertTrue(config.environment.isEmpty())
    }

    @Test
    fun `script config should accept all custom values`() {
        val config = ScriptConfig(
            command = "./deploy.sh",
            description = "Deploys application",
            phase = "PUBLISH",
            workingDirectory = "scripts",
            environment = mapOf("ENV" to "production")
        )

        assertEquals("./deploy.sh", config.command)
        assertEquals("Deploys application", config.description)
        assertEquals("PUBLISH", config.phase)
        assertEquals("scripts", config.workingDirectory)
        assertEquals(1, config.environment.size)
        assertEquals("production", config.environment["ENV"])
    }

    @Test
    fun `context should support multiple scripts`() {
        val scripts = mapOf(
            "build" to ScriptConfig(
                command = "npm run build",
                phase = "BUILD"
            ),
            "test" to ScriptConfig(
                command = "npm test",
                phase = "TEST"
            ),
            "deploy" to ScriptConfig(
                command = "./deploy.sh",
                phase = "PUBLISH"
            )
        )
        val context = ScriptsContext(scripts = scripts)

        assertEquals(3, context.scripts.size)
        assertTrue(context.scripts.containsKey("build"))
        assertTrue(context.scripts.containsKey("test"))
        assertTrue(context.scripts.containsKey("deploy"))
    }
}
