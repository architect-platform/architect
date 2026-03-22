package io.github.architectplatform.api.testing

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.impl.SimpleTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchitectPluginTestKitTest {
  @Test
  fun `configure maps plain config maps into plugin context`() {
    val plugin = ExamplePlugin()
    val kit = ArchitectPluginTestKit(plugin)

    kit.configure(mapOf("greeting" to "hello", "enabled" to true))

    assertEquals("hello", plugin.context.greeting)
    assertTrue(plugin.context.enabled)
  }

  @Test
  fun `executeTask runs registered plugin task`() {
    val plugin = ExamplePlugin()
    val kit = ArchitectPluginTestKit(plugin)
      .configure(mapOf("greeting" to "hello"))

    val result = kit.executeTask("example-task")

    assertTrue(result.success)
    assertEquals("hello from default", result.message)
  }

  @Test
  fun `withService exposes services to task execution`() {
    val plugin = ServicePlugin()
    val commandExecutor = RecordingCommandExecutor()
    val kit = ArchitectPluginTestKit(plugin)
      .withService(CommandExecutor::class.java, commandExecutor)

    val result = kit.executeTask("service-task")

    assertTrue(result.success)
    assertEquals("echo service", commandExecutor.command)
  }

  @Test
  fun `publishedEvents captures emitted events`() {
    val plugin = EventPlugin()
    val kit = ArchitectPluginTestKit(plugin)

    kit.executeTask("event-task")

    assertEquals(listOf("event-fired"), kit.publishedEvents())
  }

  data class ExampleContext(
    val greeting: String = "hi",
    val enabled: Boolean = true,
  )

  private class ExamplePlugin : ArchitectPlugin<ExampleContext> {
    override val id: String = "example-plugin"
    override val contextKey: String = "example"
    override val ctxClass: Class<ExampleContext> = ExampleContext::class.java
    override var context: ExampleContext = ExampleContext()

    override fun register(registry: TaskRegistry) {
      registry.add(
        SimpleTask(
          id = "example-task",
          description = "Example task",
        ) { environment, _ ->
          TaskResult.success("${context.greeting} from ${environment.profile()}")
        }
      )
    }
  }

  private class ServicePlugin : ArchitectPlugin<Unit> {
    override val id: String = "service-plugin"
    override val contextKey: String = "service"
    override val ctxClass: Class<Unit> = Unit::class.java
    override var context: Unit = Unit

    override fun register(registry: TaskRegistry) {
      registry.add(
        SimpleTask(
          id = "service-task",
          description = "Uses a service",
        ) { environment, _ ->
          environment.service(CommandExecutor::class.java).execute("echo service")
          TaskResult.success("service-called")
        }
      )
    }
  }

  private class EventPlugin : ArchitectPlugin<Unit> {
    override val id: String = "event-plugin"
    override val contextKey: String = "events"
    override val ctxClass: Class<Unit> = Unit::class.java
    override var context: Unit = Unit

    override fun register(registry: TaskRegistry) {
      registry.add(
        SimpleTask(
          id = "event-task",
          description = "Publishes an event",
        ) { environment, _ ->
          environment.publish("event-fired")
          TaskResult.success("done")
        }
      )
    }
  }

  private class RecordingCommandExecutor : CommandExecutor {
    var command: String? = null

    override fun execute(command: String, workingDir: String?) {
      this.command = command
    }
  }
}