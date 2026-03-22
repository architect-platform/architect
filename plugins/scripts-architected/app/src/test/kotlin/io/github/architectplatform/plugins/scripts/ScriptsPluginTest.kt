package io.github.architectplatform.plugins.scripts

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.components.workflows.hooks.HooksWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

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

    @Test
    fun `plugin resolves phases across all workflows`() {
        val plugin = ScriptsPlugin()
        plugin.context = ScriptsContext(
            scripts = linkedMapOf(
                "core" to ScriptConfig(command = "echo core", phase = "BUILD"),
                "hook" to ScriptConfig(command = "echo hook", phase = "PRE_COMMIT"),
            )
        )
        val registry = TestTaskRegistry()

        plugin.register(registry)

        assertEquals(CoreWorkflow.BUILD, registry.get("scripts-core")?.phase())
        assertEquals(HooksWorkflow.PRE_COMMIT, registry.get("scripts-hook")?.phase())
    }

    @Test
    fun `plugin chains script tasks within the same phase when sequential is enabled`() {
        val plugin = ScriptsPlugin()
        plugin.context = ScriptsContext(
            sequential = true,
            scripts = linkedMapOf(
                "build-assets" to ScriptConfig(command = "npm run build", phase = "BUILD"),
                "bundle-assets" to ScriptConfig(command = "npm run bundle", phase = "BUILD"),
                "pre-push" to ScriptConfig(command = "npm test", phase = "PRE_PUSH"),
            )
        )
        val registry = TestTaskRegistry()

        plugin.register(registry)

        assertFalse(registry.get("scripts-build-assets")?.depends()?.contains("scripts-build-assets") == true)
        assertTrue(registry.get("scripts-bundle-assets")?.depends()?.contains("scripts-build-assets") == true)
        assertFalse(registry.get("scripts-pre-push")?.depends()?.contains("scripts-bundle-assets") == true)
    }

    @Test
    fun `script task executes with working directory and environment`() {
        val commandExecutor = RecordingCommandExecutor()
        val task = ScriptTask(
            scriptName = "deploy",
            config = ScriptConfig(
                command = "./deploy.sh",
                workingDirectory = "scripts",
                environment = mapOf("ENV" to "production")
            ),
            phase = null,
            context = ScriptsContext()
        )

        val result = task.execute(
            environment = TestEnvironment(commandExecutor),
            projectContext = ProjectContext(Path.of("/repo"), emptyMap()),
            args = listOf("staging release")
        )

        assertTrue(result.success)
        assertEquals("ENV=\"production\" ./deploy.sh 'staging release'", commandExecutor.command)
        assertEquals(Path.of("/repo", "scripts").toAbsolutePath().toString(), commandExecutor.workingDir)
    }

    private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
        private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()

        override fun add(task: io.github.architectplatform.api.core.tasks.Task) {
            tasks.add(task)
        }

        override fun get(id: String) = tasks.find { it.id == id }

        override fun all() = tasks.toList()
    }

    private class RecordingCommandExecutor : CommandExecutor {
        var command: String? = null
        var workingDir: String? = null

        override fun execute(command: String, workingDir: String?) {
            this.command = command
            this.workingDir = workingDir
        }
    }

    private class TestEnvironment(
        private val commandExecutor: CommandExecutor,
    ) : Environment {
        override fun <T> service(type: Class<T>): T {
            if (type == CommandExecutor::class.java) {
                @Suppress("UNCHECKED_CAST")
                return commandExecutor as T
            }
            throw IllegalArgumentException("Unsupported service: ${type.name}")
        }

        override fun publish(event: Any) {}
    }
}
