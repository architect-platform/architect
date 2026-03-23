package io.github.architectplatform.plugins.github

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.ResourceExtractor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.plugins.github.dto.GithubContext
import io.github.architectplatform.plugins.github.dto.PipelineContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files

class GithubPluginTest {
  @Test
  fun `github-init-pipelines rejects unsafe workflow file names`() {
    val repoDir = Files.createTempDirectory("github-plugin-test")
    Files.createDirectories(repoDir.resolve(".git"))

    try {
      val plugin = GithubPlugin()
      plugin.init(
        GithubContext(
          pipelines = listOf(
            PipelineContext(
              name = "../../secrets",
              type = "ci"
            )
          )
        )
      )
      val registry = TestTaskRegistry()
      plugin.register(registry)
      val task = registry.get("github-init-pipelines")!!

      val result = task.execute(TestEnvironment(), ProjectContext(repoDir, emptyMap()), emptyList())

      assertFalse(result.success)
      assertEquals("Invalid pipeline name: '../../secrets'", result.message)
    } finally {
      repoDir.toFile().deleteRecursively()
    }
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }

  private class TestEnvironment : Environment {
    override fun <T> service(type: Class<T>): T {
      if (type == ResourceExtractor::class.java || type == CommandExecutor::class.java) {
        throw IllegalStateException("Service should not be requested for invalid workflow names")
      }
      throw IllegalArgumentException("Unsupported service: ${type.name}")
    }

    override fun publish(event: Any) {}
  }
}
