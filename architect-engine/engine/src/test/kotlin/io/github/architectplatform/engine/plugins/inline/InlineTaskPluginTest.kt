package io.github.architectplatform.engine.plugins.inline

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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
}
