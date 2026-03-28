package io.github.architectplatform.cli.embedded

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class EmbeddedTaskExecutorTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `plan resolves configured task groups and wildcard aliases`() {
    val projectDir = tempDir.resolve("grouped-cli-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: grouped-cli-project
      tasks:
        frontend-build:
          run: echo frontend
        backend-build:
          run: echo backend
      groups:
        build: [frontend-build, backend-build]
      """.trimIndent()
    )

    val executor = EmbeddedTaskExecutor(JdkRemoteContentFetcher())
    val listedTasks = executor.listTasks("grouped-cli-project", projectDir.toString())
    val listedIds = listedTasks.map { it.id }
    val groupHeader = listedTasks.firstOrNull { it.id == "build" }
    assertNotNull(groupHeader)
    assertEquals(listOf("frontend-build", "backend-build"), groupHeader!!.groupMembers)
    assertTrue("frontend-build" in listedIds)
    assertTrue("backend-build" in listedIds)
    assertTrue("build:frontend" !in listedIds)
    assertTrue("build:backend" !in listedIds)

    val groupPlan = executor.plan("grouped-cli-project", projectDir.toString(), "build")
    assertEquals(setOf("frontend-build", "backend-build", "build"), groupPlan.steps.map { it.id }.toSet())

    val wildcardPlan = executor.plan("grouped-cli-project", projectDir.toString(), "build:*")
    assertTrue(wildcardPlan.steps.any { it.id == "frontend-build" })
    assertTrue(wildcardPlan.steps.any { it.id == "backend-build" })
  }
}
