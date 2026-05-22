@file:Suppress("MaxLineLength")
package io.github.architectplatform.core.plugins.inline

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.Config
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.core.events.EmbeddedEventBus
import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.tasks.application.TaskCache
import io.github.architectplatform.core.tasks.application.TaskExecutor
import io.github.architectplatform.core.tasks.infrastructure.InMemoryTaskRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class InlineTaskPluginTest {

    @Test
    fun `should register tasks defined in tasks section`() {
        // Arrange
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf(
            "deploy" to mapOf(
                "description" to "Deploy to staging",
                "run" to "kubectl apply -f k8s/",
                "phase" to "PUBLISH",
                "depends" to listOf("build", "test"),
            ),
            "smoke-test" to mapOf(
                "description" to "Smoke test",
                "run" to "curl -f https://example.com/health",
            ),
        ))
        val registry = InMemoryTaskRegistry()

        // Act
        plugin.register(registry)

        // Assert
        assertEquals(2, registry.all().size)
        val deploy = registry.get("deploy")
        assertNotNull(deploy)
        assertEquals("Deploy to staging", deploy!!.description())
        assertEquals(listOf("release", "build", "test"), deploy.depends())
    }

    @Test
    fun `should preserve existing phase permissions and requires behavior`() {
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf(
            "deploy" to mapOf(
                "description" to "Deploy to staging",
                "run" to "kubectl apply -f k8s/",
                "phase" to "PUBLISH",
                "depends" to listOf("build", "test"),
                "permissions" to listOf("file-system:read", "network:outbound", "process:exec"),
                "requires" to mapOf(
                    "tools" to listOf("kubectl"),
                    "min-tool-versions" to mapOf("kubectl" to "1.30.0"),
                    "env" to listOf("KUBECONFIG"),
                    "platform" to listOf("linux", "darwin"),
                ),
            ),
        ))
        val registry = InMemoryTaskRegistry()

        plugin.register(registry)

        val task = registry.get("deploy")
        assertNotNull(task)
        assertEquals(CoreWorkflow.PUBLISH, task!!.phase())
        assertEquals(listOf("release", "build", "test"), task.depends())
        assertEquals(
            setOf(TaskPermission.FILE_SYSTEM_READ, TaskPermission.NETWORK_OUTBOUND, TaskPermission.PROCESS_EXEC),
            task.requiredPermissions(),
        )
        assertEquals(listOf("kubectl"), task.requires()!!.tools)
        assertEquals(mapOf("kubectl" to "1.30.0"), task.requires()!!.minToolVersions)
        assertEquals(listOf("KUBECONFIG"), task.requires()!!.env)
        assertEquals(setOf(io.github.architectplatform.api.core.tasks.Platform.LINUX, io.github.architectplatform.api.core.tasks.Platform.DARWIN), task.requires()!!.platform)
    }

    @Test
    fun `should skip tasks with no run command`() {
        // Arrange
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf(
            "no-run" to mapOf("description" to "missing run"),
            "with-run" to mapOf("run" to "echo hello"),
        ))
        val registry = InMemoryTaskRegistry()

        // Act
        plugin.register(registry)

        // Assert
        assertEquals(1, registry.all().size)
        assertNotNull(registry.get("with-run"))
        assertNull(registry.get("no-run"))
    }

    @Test
    fun `should resolve CoreWorkflow phases by name`() {
        // Arrange
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf(
            "my-build" to mapOf("run" to "make", "phase" to "BUILD"),
            "my-test" to mapOf("run" to "make test", "phase" to "TEST"),
        ))
        val registry = InMemoryTaskRegistry()

        // Act
        plugin.register(registry)

        // Assert
        assertEquals(CoreWorkflow.BUILD, registry.get("my-build")?.phase())
        assertEquals(CoreWorkflow.TEST, registry.get("my-test")?.phase())
    }

    @Test
    fun `should default description to task id when not provided`() {
        // Arrange
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf("my-task" to mapOf("run" to "echo hi")))
        val registry = InMemoryTaskRegistry()

        // Act
        plugin.register(registry)

        // Assert
        val task = registry.get("my-task")
        assertNotNull(task)
        assert(task!!.description().contains("my-task"))
    }

    @Test
    fun `should register no tasks when context is empty`() {
        // Arrange
        val plugin = InlineTaskPlugin()
        plugin.init(emptyMap<String, Any>())
        val registry = InMemoryTaskRegistry()

        // Act
        plugin.register(registry)

        // Assert
        assertEquals(0, registry.all().size)
    }

    @Test
    fun `should expose declared permissions on inline tasks`() {
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf(
            "deploy" to mapOf(
                "run" to "kubectl apply -f k8s/",
                "permissions" to listOf("file-system:read", "network:outbound", "process:exec"),
            ),
        ))
        val registry = InMemoryTaskRegistry()

        plugin.register(registry)

        assertEquals(
            setOf(TaskPermission.FILE_SYSTEM_READ, TaskPermission.NETWORK_OUTBOUND, TaskPermission.PROCESS_EXEC),
            registry.get("deploy")!!.requiredPermissions(),
        )
    }

    @Test
    fun `config schema documents extended inline task fields`() {
        val schema = InlineTaskPlugin().configSchema()
        @Suppress("UNCHECKED_CAST")
        val taskProperties = ((schema["additionalProperties"] as Map<String, Any>)["properties"] as Map<String, Any>)

        assertTrue(taskProperties.containsKey("run"))
        assertTrue(taskProperties.containsKey("extends"))
        assertTrue(taskProperties.containsKey("phase"))
        assertTrue(taskProperties.containsKey("depends"))
        assertTrue(taskProperties.containsKey("permissions"))
        assertTrue(taskProperties.containsKey("requires"))
        assertTrue(taskProperties.containsKey("condition"))
        assertTrue(taskProperties.containsKey("timeout"))
        assertTrue(taskProperties.containsKey("onFailure"))
        assertTrue(taskProperties.containsKey("retryAttempts"))
    }

    @Test
    fun `should apply template defaults before registering tasks`() {
        val plugin = InlineTaskPlugin()
        plugin.init(
            mapOf(
                "templates" to mapOf(
                    "npm-script" to mapOf(
                        "timeout" to "120s",
                        "requires" to mapOf("tools" to listOf("node", "npm")),
                        "permissions" to listOf("process:exec"),
                    ),
                ),
                "tasks" to mapOf(
                    "frontend-build" to mapOf(
                        "extends" to "npm-script",
                        "run" to "npm run build",
                        "phase" to "BUILD",
                    ),
                ),
            ),
        )
        val registry = InMemoryTaskRegistry()

        plugin.register(registry)

        val task = registry.get("frontend-build")
        assertNotNull(task)
        assertEquals(CoreWorkflow.BUILD, task!!.phase())
        assertEquals(listOf("node", "npm"), task.requires()!!.tools)
        assertEquals(setOf(TaskPermission.PROCESS_EXEC), task.requiredPermissions())
        assertEquals(java.time.Duration.ofSeconds(120), task.timeout())
    }

    @Test
    fun `should let task values override template defaults`() {
        val plugin = InlineTaskPlugin()
        plugin.init(
            mapOf(
                "templates" to mapOf(
                    "npm-script" to mapOf(
                        "run" to "npm run lint",
                        "timeout" to "60s",
                    ),
                ),
                "tasks" to mapOf(
                    "frontend-build" to mapOf(
                        "extends" to "npm-script",
                        "run" to "npm run build",
                        "timeout" to "180s",
                    ),
                ),
            ),
        )
        val registry = InMemoryTaskRegistry()

        plugin.register(registry)

        val task = registry.get("frontend-build")
        assertNotNull(task)
        assertEquals(java.time.Duration.ofSeconds(180), task!!.timeout())
    }

    @Test
    fun `should fail when task extends unknown template`() {
        val plugin = InlineTaskPlugin()
        plugin.init(
            mapOf(
                "templates" to emptyMap<String, Any>(),
                "tasks" to mapOf(
                    "frontend-build" to mapOf(
                        "extends" to "missing-template",
                        "run" to "npm run build",
                    ),
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            plugin.register(InMemoryTaskRegistry())
        }
    }

    @Test
    fun `should fail on circular template inheritance`() {
        val plugin = InlineTaskPlugin()
        plugin.init(
            mapOf(
                "templates" to mapOf(
                    "base" to mapOf("extends" to "shared"),
                    "shared" to mapOf("extends" to "base"),
                ),
                "tasks" to mapOf(
                    "frontend-build" to mapOf(
                        "extends" to "base",
                        "run" to "npm run build",
                    ),
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            plugin.register(InMemoryTaskRegistry())
        }
    }

    @Test
    fun `should execute inline task successfully`(@TempDir tmpDir: Path) {
        val commandExecutor = RecordingCommandExecutor()
        val registry = registryFor(
            "deploy" to mapOf(
                "run" to "echo deploy",
                "condition" to "env.BRANCH == 'main'",
            ),
        )
        val task = registry.get("deploy")!!

        val result = executeTask(tmpDir, registry, task, commandExecutor, variables = mapOf("BRANCH" to "main"))

        assertTrue(result.success)
        assertEquals(TaskResult.Status.SUCCESS, result.status)
        assertEquals(listOf("echo deploy"), commandExecutor.commands)
    }

    @Test
    fun `should skip inline task when condition is false`(@TempDir tmpDir: Path) {
        val commandExecutor = RecordingCommandExecutor()
        val registry = registryFor(
            "publish" to mapOf(
                "run" to "echo publish",
                "condition" to "env.BRANCH == 'main'",
            ),
        )
        val task = registry.get("publish")!!

        val result = executeTask(tmpDir, registry, task, commandExecutor, variables = mapOf("BRANCH" to "feature/test"))

        assertFalse(task.shouldExecute(TestEnvironment(commandExecutor, mapOf("BRANCH" to "feature/test")), ProjectContext(dir = tmpDir, config = config())))
        assertTrue(result.success)
        assertEquals(TaskResult.Status.SKIPPED, result.status)
        assertTrue(result.message!!.contains("skipped"))
        assertTrue(commandExecutor.commands.isEmpty())
    }

    @Test
    fun `should retry inline task when configured with retry attempts`(@TempDir tmpDir: Path) {
        val commandExecutor = FlakyCommandExecutor(failuresBeforeSuccess = 2)
        val registry = registryFor(
            "deploy" to mapOf(
                "run" to "echo deploy",
                "onFailure" to "RETRY",
                "retryAttempts" to 2,
            ),
        )
        val task = registry.get("deploy")!!

        val result = executeTask(tmpDir, registry, task, commandExecutor)

        assertTrue(result.success)
        assertEquals(3, commandExecutor.attempts.get())
    }

    @Test
    fun `should timeout inline task when task exceeds configured timeout`(@TempDir tmpDir: Path) {
        val commandExecutor = SleepingCommandExecutor(sleepMillis = 500)
        val registry = registryFor(
            "slow" to mapOf(
                "run" to "sleep forever",
                "timeout" to "100ms",
            ),
        )
        val task = registry.get("slow")!!

        val result = executeTask(tmpDir, registry, task, commandExecutor)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("timed out"))
        assertEquals(1, commandExecutor.attempts.get())
    }

    private fun registryFor(vararg tasks: Pair<String, Map<String, Any>>): InMemoryTaskRegistry {
        val plugin = InlineTaskPlugin()
        plugin.init(mapOf(*tasks))
        return InMemoryTaskRegistry().also(plugin::register)
    }

    private fun executeTask(
        dir: Path,
        registry: InMemoryTaskRegistry,
        task: io.github.architectplatform.api.core.tasks.Task,
        commandExecutor: CommandExecutor,
        variables: Map<String, String> = emptyMap(),
    ): TaskResult {
        val eventBus = EmbeddedEventBus<io.github.architectplatform.core.domain.events.ArchitectEvent<*>>()
        val environment = TestEnvironment(commandExecutor, variables)
        val executor = TaskExecutor(
            environment = environment,
            taskCache = TaskCache(cacheEnabled = false),
            eventBus = eventBus::invoke,
            parallelExecutionEnabled = false,
        )
        val project = Project(
            name = "inline-test",
            path = dir.toString(),
            context = ProjectContext(dir = dir, config = config()),
            plugins = emptyList(),
            taskRegistry = registry,
        )

        val (_, deferred) = executor.execute(project, task, project.context, emptyList())
        return runBlocking { deferred.await() }
    }

    private fun config(): Config = mapOf("project" to mapOf("name" to "inline-test"))

    private class TestEnvironment(
        private val commandExecutor: CommandExecutor,
        private val variables: Map<String, String>,
    ) : Environment {
        override fun <T> service(type: Class<T>): T {
            if (type == CommandExecutor::class.java) {
                @Suppress("UNCHECKED_CAST")
                return commandExecutor as T
            }
            throw IllegalArgumentException("Unsupported service: ${type.name}")
        }

        override fun publish(event: Any) {}

        override fun variable(name: String): String? = variables[name]
    }

    private open class RecordingCommandExecutor : CommandExecutor {
        val commands = mutableListOf<String>()

        override fun execute(command: String, workingDir: String?) {
            commands += command
        }
    }

    private class FlakyCommandExecutor(
        private val failuresBeforeSuccess: Int,
    ) : RecordingCommandExecutor() {
        val attempts = AtomicInteger(0)

        override fun execute(command: String, workingDir: String?) {
            super.execute(command, workingDir)
            if (attempts.getAndIncrement() < failuresBeforeSuccess) {
                error("boom")
            }
        }
    }

    private class SleepingCommandExecutor(
        private val sleepMillis: Long,
    ) : RecordingCommandExecutor() {
        val attempts = AtomicInteger(0)

        override fun execute(command: String, workingDir: String?) {
            attempts.incrementAndGet()
            super.execute(command, workingDir)
            try {
                Thread.sleep(sleepMillis)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
