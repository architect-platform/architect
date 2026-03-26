package io.github.architectplatform.engine.integration

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.core.history.app.HistoryService
import io.github.architectplatform.core.project.app.ConfigLoader
import io.github.architectplatform.core.project.app.ConfigValidator
import io.github.architectplatform.core.project.app.ProjectService
import io.github.architectplatform.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.core.project.infra.YamlConfigParser
import io.github.architectplatform.engine.core.tasks.application.ExecutionEventCollector
import io.github.architectplatform.core.tasks.application.TaskCache
import io.github.architectplatform.core.tasks.application.TaskExecutor
import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.plugins.inline.InlineTaskPlugin
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.micronaut.context.event.ApplicationEventPublisher
import io.github.architectplatform.core.plugin.app.PluginLoader

class MonorepoExecutionIntegrationTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should execute subproject tasks in parallel before root task and persist successful history`() = runBlocking {
    withTemporaryHome {
      val tracker = TrackingCommandExecutor()
      val fixture = createFixture(tracker)
      val workspaceDir = createWorkspace(
        projectName = "monorepo-success",
        rootCommand = "echo root-build",
        subprojects = mapOf(
          "service-a" to "echo service-a-build",
          "service-b" to "echo service-b-build",
        ),
      )

      fixture.projectService.registerProject("monorepo-success", workspaceDir.toString())

      val executionId = fixture.taskService.executeTask("monorepo-success", "build", emptyList())
      val events = awaitExecutionEvents(fixture.eventCollector, executionId)
      val history = fixture.historyService.getByProject("monorepo-success", limit = 1).first()

      assertTrue(tracker.maxConcurrent.get() >= 2, "Expected subprojects to execute in parallel")
      assertEquals(listOf("service-a", "service-b", "monorepo-success"), tracker.completedProjects.sortedBy { expectedOrder(it) })
      assertTrue(tracker.startedProjects.take(2).toSet() == setOf("service-a", "service-b"))
      assertEquals("monorepo-success", tracker.startedProjects.last())

      val childStarts = events.filter {
        it.id == "task.started" &&
          it.event?.parentProject == "monorepo-success"
      }
      val rootStarts = events.filter {
        it.id == "task.started" &&
          it.event?.project == "monorepo-success" &&
          it.event?.parentProject == null
      }
      assertEquals(2, childStarts.size)
      assertEquals(1, rootStarts.size)
      assertEquals("execution.completed", events.last().id)

      assertTrue(history.success)
      assertEquals("build", history.task)
      assertTrue(history.message.orEmpty().contains("All tasks completed successfully"))
    }
  }

  @Test
  fun `should aggregate subproject failures and skip root task execution`() = runBlocking {
    withTemporaryHome {
      val tracker = TrackingCommandExecutor(failingProjects = setOf("service-b"))
      val fixture = createFixture(tracker)
      val workspaceDir = createWorkspace(
        projectName = "monorepo-failure",
        rootCommand = "echo root-build",
        subprojects = mapOf(
          "service-a" to "echo service-a-build",
          "service-b" to "echo service-b-build",
        ),
      )

      fixture.projectService.registerProject("monorepo-failure", workspaceDir.toString())

      val executionId = fixture.taskService.executeTask("monorepo-failure", "build", emptyList())
      val events = awaitExecutionEvents(fixture.eventCollector, executionId)
      val history = fixture.historyService.getByProject("monorepo-failure", limit = 1).first()

      assertTrue(tracker.maxConcurrent.get() >= 2, "Expected subprojects to begin in parallel")
      assertFalse(tracker.startedProjects.contains("monorepo-failure"), "Root task should not execute when a subproject fails")
      assertEquals("execution.failed", events.last().id)
      assertTrue(events.any { it.id == "task.failed" && it.event?.project == "service-b" })

      assertFalse(history.success)
      assertEquals("Some subprojects failed", history.message)
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

  private fun createFixture(commandExecutor: TrackingCommandExecutor): Fixture {
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
      pluginLoader = InlineTaskPluginLoader(),
      projectReporter = Optional.empty(),
      configValidator = ConfigValidator(),
    )
    val taskExecutor = TaskExecutor(
      environment = environment,
      taskCache = TaskCache(cacheEnabled = false),
      eventBus = { event -> eventPublisher.publishEvent(event) },
      parallelExecutionEnabled = true,
    )
    val historyService = HistoryService()
    val taskService = TaskService(projectService, taskExecutor, eventCollector, eventPublisher, historyService, io.github.architectplatform.engine.core.metrics.MetricsService())

    return Fixture(projectService, taskService, eventCollector, historyService)
  }

  private fun createWorkspace(
    projectName: String,
    rootCommand: String,
    subprojects: Map<String, String>,
  ): Path {
    val workspaceDir = tempDir.resolve(projectName)
    workspaceDir.createDirectories()
    writeProjectConfig(workspaceDir, projectName, rootCommand)
    subprojects.forEach { (name, command) ->
      val subprojectDir = workspaceDir.resolve(name)
      subprojectDir.createDirectories()
      writeProjectConfig(subprojectDir, name, command)
    }
    return workspaceDir
  }

  private fun writeProjectConfig(projectDir: Path, projectName: String, command: String) {
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: $projectName
      tasks:
        build:
          description: Build $projectName
          run: $command
      """.trimIndent() + "\n"
    )
  }

  private fun expectedOrder(projectName: String): Int = when (projectName) {
    "service-a" -> 0
    "service-b" -> 1
    else -> 2
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

  private class InlineTaskPluginLoader : PluginLoader {
    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> = listOf(InlineTaskPlugin())
  }

  private class TrackingCommandExecutor(
    private val failingProjects: Set<String> = emptySet(),
  ) : CommandExecutor {
    val startedProjects = CopyOnWriteArrayList<String>()
    val completedProjects = CopyOnWriteArrayList<String>()
    val maxConcurrent = AtomicInteger(0)
    private val activeExecutions = AtomicInteger(0)

    override fun execute(command: String, workingDir: String?) {
      val projectName = Path.of(workingDir ?: error("Missing working directory")).fileName.toString()
      startedProjects += projectName

      val concurrent = activeExecutions.incrementAndGet()
      maxConcurrent.updateAndGet { current -> maxOf(current, concurrent) }

      try {
        Thread.sleep(150)
        if (projectName in failingProjects) {
          throw IllegalStateException("Synthetic failure for $projectName")
        }
      } finally {
        activeExecutions.decrementAndGet()
        completedProjects += projectName
      }
    }
  }
}