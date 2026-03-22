package io.github.architectplatform.engine.integration

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@MicronautTest
class CliEngineHttpIntegrationTest {

  @Inject
  lateinit var client: EngineCommandClient

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should execute inline task through cli http client and stream completion events`() = runBlocking {
    val projectName = uniqueProjectName("cli-engine-success")
    val projectPath = createProject(
      projectName,
      """
      tasks:
        hello:
          description: hello task
          run: echo hello-from-integration
      """.trimIndent(),
    )

    client.registerProject(RegisterProjectRequest(projectName, projectPath))

    val executionId = client.execute(projectName, "hello", emptyList())
    val events = withTimeout(10_000) { client.getExecutionFlow(executionId).toList() }
    val eventIds = events.mapNotNull { it["id"] as? String }
    val eventTypes = events.mapNotNull { (it["event"] as? Map<*, *>)?.get("executionEventType") as? String }

    assertTrue(eventIds.contains("execution.started"))
    assertTrue(eventIds.contains("task.started"))
    assertTrue(eventIds.contains("task.completed"))
    assertEquals("execution.completed", eventIds.last())
    assertEquals("COMPLETED", eventTypes.last())
  }

  @Test
  fun `should execute failing inline task through cli http client and stream failure events`() = runBlocking {
    val projectName = uniqueProjectName("cli-engine-failure")
    val projectPath = createProject(
      projectName,
      """
      tasks:
        explode:
          description: failing task
          run: sh -c 'echo boom >&2; exit 1'
      """.trimIndent(),
    )

    client.registerProject(RegisterProjectRequest(projectName, projectPath))

    val executionId = client.execute(projectName, "explode", emptyList())
    val events = withTimeout(10_000) { client.getExecutionFlow(executionId).toList() }
    val eventIds = events.mapNotNull { it["id"] as? String }
    val terminalEvent = events.last()["event"] as Map<*, *>

    assertTrue(eventIds.contains("execution.started"))
    assertTrue(eventIds.contains("task.failed"))
    assertEquals("execution.failed", eventIds.last())
    assertEquals("FAILED", terminalEvent["executionEventType"])
    assertTrue((terminalEvent["message"] as? String).orEmpty().contains("Execution failed"))
  }

  private fun createProject(projectName: String, additionalYaml: String): String {
    val projectDir = File(tempDir.toFile(), projectName)
    projectDir.mkdirs()
    File(projectDir, "architect.yml").writeText(
      buildString {
        appendLine("project:")
        appendLine("  name: $projectName")
        appendLine(additionalYaml)
      }
    )
    return projectDir.absolutePath
  }

  private fun uniqueProjectName(prefix: String): String = "$prefix-${UUID.randomUUID().toString().take(8)}"
}