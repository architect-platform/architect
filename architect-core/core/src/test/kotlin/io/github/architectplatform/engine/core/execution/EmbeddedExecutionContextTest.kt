package io.github.architectplatform.engine.core.execution

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.engine.core.history.app.HistoryService
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class EmbeddedExecutionContextTest {

  private val remoteFetcher =
    object : RemoteContentFetcher {
      override fun fetchText(url: String, headers: Map<String, String>): String {
        error("Network fetch should not be used in this test: $url")
      }

      override fun fetchBytes(url: String, headers: Map<String, String>): ByteArray {
        error("Network fetch should not be used in this test: $url")
      }
    }

  @Test
  fun `should wire all core services and plugin sources`() {
    val context = EmbeddedExecutionContext.create(remoteContentFetcher = remoteFetcher)

    assertNotNull(context.eventBus)
    assertNotNull(context.environment)
    assertNotNull(context.taskExecutor)
    assertNotNull(context.projectService)
    assertNotNull(context.historyService)
    assertNotNull(context.pluginSourceRegistry)

    val supportedSources = context.pluginSourceRegistry.getSupportedTypes().toSet()
    assertEquals(setOf("github", "local"), supportedSources)

    val commandExecutor = context.environment.service(CommandExecutor::class.java)
    val historyService = context.environment.service(HistoryService::class.java)
    assertNotNull(commandExecutor)
    assertNotNull(historyService)
  }

  @Test
  fun `should register project and execute inline task in embedded mode`() {
    val projectDir = createTempDirectory("embedded-context-test")
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: embedded-context-project
      tasks:
        hello:
          description: hello task
          run: echo hello-from-embedded
      """.trimIndent()
    )

    val context = EmbeddedExecutionContext.create(remoteContentFetcher = remoteFetcher)
    val observedEventIds = mutableListOf<String>()
    val unsubscribe = context.eventBus.subscribe { event: ArchitectEvent<*> ->
      observedEventIds += event.id
    }

    context.projectService.registerProject("embedded-context-project", projectDir.toString())
    val project = context.projectService.getProject("embedded-context-project")
    assertNotNull(project)

    val task = project.taskRegistry.get("hello")
    assertNotNull(task)

    val (_, deferred) = context.taskExecutor.execute(project, task, project.context, emptyList())
    val result = runBlocking { deferred.await() }

    unsubscribe()

    assertTrue(result.success, result.message ?: "Expected embedded task execution to succeed")
    assertTrue(observedEventIds.any { it == "task.started" })
    assertTrue(observedEventIds.any { it == "task.completed" })
  }
}
