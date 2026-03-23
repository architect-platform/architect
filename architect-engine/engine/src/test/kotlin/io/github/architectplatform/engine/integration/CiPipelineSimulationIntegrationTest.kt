package io.github.architectplatform.engine.integration

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.engine.core.history.app.HistoryService
import io.github.architectplatform.engine.core.project.app.ConfigLoader
import io.github.architectplatform.engine.core.project.app.ConfigValidator
import io.github.architectplatform.engine.core.project.app.ProjectService
import io.github.architectplatform.engine.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.engine.core.project.infra.YamlConfigParser
import io.github.architectplatform.engine.core.tasks.application.ExecutionEventCollector
import io.github.architectplatform.engine.core.tasks.application.TaskCache
import io.github.architectplatform.engine.core.tasks.application.TaskExecutor
import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.plugins.inline.InlineTaskPlugin
import io.github.architectplatform.engine.plugins.workflows.core.CorePlugin
import io.micronaut.context.event.ApplicationEventPublisher
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.github.architectplatform.engine.core.plugin.app.PluginLoader

class CiPipelineSimulationIntegrationTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should execute init lint verify build test pipeline in order and persist successful history`() = runBlocking {
    withTemporaryHome {
      val commandExecutor = RecordingCommandExecutor()
      val fixture = createFixture(commandExecutor)
      val projectDir = tempDir.resolve("architect-ci-simulation")
      projectDir.createDirectories()
      projectDir.resolve("architect.yml").writeText(
        """
        project:
          name: architect-ci-simulation
        tasks:
          ci-init-step:
            description: Prepare the workspace
            phase: INIT
            run: ci-init
          ci-lint-step:
            description: Lint sources
            phase: LINT
            run: ci-lint
          ci-verify-step:
            description: Verify configuration
            phase: VERIFY
            run: ci-verify
          ci-build-step:
            description: Build artifacts
            phase: BUILD
            run: ci-build
          ci-test-step:
            description: Run tests
            phase: TEST
            run: ci-test
        """.trimIndent() + "\n"
      )

      fixture.projectService.registerProject("architect-ci-simulation", projectDir.toString())

      val executionId = fixture.taskService.executeTask("architect-ci-simulation", "test", emptyList())
      val events = awaitExecutionEvents(fixture.eventCollector, executionId)
      val history = fixture.historyService.getByProject("architect-ci-simulation", limit = 1).first()

      assertEquals(
        listOf("ci-init", "ci-lint", "ci-verify", "ci-build", "ci-test"),
        commandExecutor.commands,
      )
      assertEquals("execution.completed", events.last().id)
      assertTrue(events.any { it.id == "task.started" && it.event?.project == "architect-ci-simulation" })
      assertTrue(history.success)
      assertEquals("test", history.task)
      assertTrue(history.message.orEmpty().contains("All tasks completed successfully"))
    }
  }

  private suspend fun awaitExecutionEvents(
    eventCollector: ExecutionEventCollector,
    executionId: String,
  ): List<ArchitectEvent<ExecutionEvent>> {
    val events = CopyOnWriteArrayList<ArchitectEvent<ExecutionEvent>>()
    val terminalEventSeen = CompletableDeferred<Unit>()

    val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      eventCollector.getFlow(executionId).collect { event ->
        events += event
        if (event.id == "execution.completed" || event.id == "execution.failed") {
          terminalEventSeen.complete(Unit)
        }
      }
    }

    withTimeout(10_000) {
      terminalEventSeen.await()
    }
    job.cancelAndJoin()
    return events.toList()
  }

  private fun createFixture(commandExecutor: RecordingCommandExecutor): Fixture {
    val eventCollector = ExecutionEventCollector()
    val eventPublisher = mock<ApplicationEventPublisher<ArchitectEvent<*>>>()
    doAnswer { invocation ->
      val event = invocation.getArgument<ArchitectEvent<*>>(0)
      eventCollector.onExecutionEvent(event)
      null
    }.whenever(eventPublisher).publishEvent(any())

    val environment = object : Environment {
      override fun <T> service(type: Class<T>): T {
        if (type == CommandExecutor::class.java) {
          @Suppress("UNCHECKED_CAST")
          return commandExecutor as T
        }
        throw IllegalArgumentException("Unsupported service: ${type.name}")
      }

      override fun publish(event: Any) {}
    }

    val projectService = ProjectService(
      projectRepository = InMemoryProjectRepository(),
      configLoader = ConfigLoader(YamlConfigParser()),
      pluginLoader = PipelinePluginLoader(),
      cloudReporter = Optional.empty(),
      configValidator = ConfigValidator(),
    )
    val taskExecutor = TaskExecutor(
      environment = environment,
      taskCache = TaskCache(cacheEnabled = false),
      eventPublisher = eventPublisher,
      parallelExecutionEnabled = true,
    )
    val historyService = HistoryService()
    val taskService = TaskService(projectService, taskExecutor, eventCollector, eventPublisher, historyService)

    return Fixture(projectService, taskService, eventCollector, historyService)
  }

  private inline fun withTemporaryHome(block: () -> Unit) {
    val homeDir = tempDir.resolve("home")
    homeDir.createDirectories()
    val originalHome = System.getProperty("user.home")
    System.setProperty("user.home", homeDir.toString())
    try {
      block()
    } finally {
      System.setProperty("user.home", originalHome)
    }
  }

  private data class Fixture(
    val projectService: ProjectService,
    val taskService: TaskService,
    val eventCollector: ExecutionEventCollector,
    val historyService: HistoryService,
  )

  private class PipelinePluginLoader : PluginLoader {
    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> = listOf(CorePlugin(), InlineTaskPlugin())
  }

  private class RecordingCommandExecutor : CommandExecutor {
    val commands = CopyOnWriteArrayList<String>()

    override fun execute(command: String, workingDir: String?) {
      commands += command
    }
  }
}