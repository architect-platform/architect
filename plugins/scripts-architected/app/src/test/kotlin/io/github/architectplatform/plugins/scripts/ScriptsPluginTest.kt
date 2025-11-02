package io.github.architectplatform.plugins.scripts

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScriptsPluginTest {

    @Test
    fun `plugin should have correct metadata`() {
        val plugin = ScriptsPlugin()

        assertEquals("scripts-plugin", plugin.id)
        assertEquals("scripts", plugin.contextKey)
        assertEquals(ScriptsContext::class.java, plugin.ctxClass)
    }

    @Test
    fun `plugin should have default empty context`() {
        val plugin = ScriptsPlugin()

        assertNotNull(plugin.context)
        assertTrue(plugin.context.enabled)
        assertTrue(plugin.context.scripts.isEmpty())
    }

    @Test
    fun `plugin should support context update`() {
        val plugin = ScriptsPlugin()
        val newContext = ScriptsContext(
            enabled = true,
            scripts = mapOf(
                "hello" to ScriptConfig(command = "echo 'Hello'")
            )
        )

        plugin.context = newContext

        assertEquals(newContext, plugin.context)
        assertEquals(1, plugin.context.scripts.size)
    }

    @Test
    fun `script task should have correct id format`() {
        val task = ScriptTask(
            scriptName = "my-script",
            config = ScriptConfig(command = "echo test"),
            phase = null,
            context = ScriptsContext()
        )

        assertEquals("scripts-my-script", task.id)
    }

    @Test
    fun `script task should use configured description`() {
        val task = ScriptTask(
            scriptName = "deploy",
            config = ScriptConfig(
                command = "./deploy.sh",
                description = "Deploys the application"
            ),
            phase = null,
            context = ScriptsContext()
        )

        assertEquals("Deploys the application", task.description())
    }

    @Test
    fun `script task should have correct phase when specified`() {
        val task = ScriptTask(
            scriptName = "build",
            config = ScriptConfig(command = "npm run build"),
            phase = CodeWorkflow.BUILD,
            context = ScriptsContext()
        )

        assertEquals(CodeWorkflow.BUILD, task.phase())
    }

    @Test
    fun `script task should have no phase when not specified`() {
        val task = ScriptTask(
            scriptName = "standalone",
            config = ScriptConfig(command = "echo test"),
            phase = null,
            context = ScriptsContext()
        )

        assertNull(task.phase())
    }
}
