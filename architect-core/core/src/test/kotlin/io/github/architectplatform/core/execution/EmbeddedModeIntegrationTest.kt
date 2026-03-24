package io.github.architectplatform.core.execution

import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class EmbeddedModeIntegrationTest {

  private val remoteFetcher =
    object : RemoteContentFetcher {
      override fun fetchText(url: String, headers: Map<String, String>): String {
        error("Network fetch should not be used in integration test: $url")
      }

      override fun fetchBytes(url: String, headers: Map<String, String>): ByteArray {
        error("Network fetch should not be used in integration test: $url")
      }
    }

  @Test
  fun `should load real inline plugin and execute inline task in embedded mode`() {
    val projectDir = createTempDirectory("embedded-e2e")
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: embedded-e2e-project
      tasks:
        e2e-hello:
          description: embedded end-to-end hello task
          run: echo embedded-e2e-ok
      """.trimIndent()
    )

    val context = EmbeddedExecutionContext.create(remoteContentFetcher = remoteFetcher)
    context.projectService.registerProject("embedded-e2e-project", projectDir.toString())

    val project = context.projectService.getProject("embedded-e2e-project")
    assertNotNull(project)

    val pluginIds = project.plugins.map { it.id }.toSet()
    assertTrue(pluginIds.contains("inline-tasks"), "Expected inline-tasks plugin to be loaded")

    val task = project.taskRegistry.get("e2e-hello")
    assertNotNull(task)

    val (_, deferred) = context.taskExecutor.execute(project, task, project.context, emptyList())
    val result = runBlocking { deferred.await() }

    assertTrue(result.success, result.message ?: "Expected end-to-end embedded task to succeed")
    assertTrue(
      (result.message ?: "").contains("completed", ignoreCase = true),
      "Expected success message to indicate completion, actual: ${result.message}",
    )
  }
}
