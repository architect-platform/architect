package io.github.architectplatform.engine.plugins.commits.tasks

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.engine.plugins.commits.context.CommitsContext
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerifyCommitMessageTaskTest {
  private val task = VerifyCommitMessageTask(CommitsContext())

  @Test
  fun `execute rejects commit message path that escapes project root`() {
    val repoDir = Files.createTempDirectory("core-commit-msg")
    val projectDir = Files.createDirectories(repoDir.resolve("module"))
    Files.createDirectories(repoDir.resolve(".git"))
    Files.writeString(repoDir.parent.resolve("COMMIT_EDITMSG"), "feat(test): valid message")

    try {
      val result = task.execute(TestEnvironment(), ProjectContext(projectDir, emptyMap()), listOf("../COMMIT_EDITMSG"))

      assertFalse(result.success)
      assertTrue(result.message!!.contains("Invalid commit message file path"))
    } finally {
      repoDir.toFile().deleteRecursively()
      repoDir.parent.resolve("COMMIT_EDITMSG").toFile().delete()
    }
  }

  @Test
  fun `execute reads commit message file inside project root`() {
    val repoDir = Files.createTempDirectory("core-commit-msg")
    val projectDir = Files.createDirectories(repoDir.resolve("module"))
    Files.createDirectories(repoDir.resolve(".git"))
    Files.writeString(repoDir.resolve("COMMIT_EDITMSG"), "feat(test): valid message")

    try {
      val result = task.execute(TestEnvironment(), ProjectContext(projectDir, emptyMap()), listOf("COMMIT_EDITMSG"))

      assertTrue(result.success)
      assertTrue(result.message!!.contains("verified successfully"))
    } finally {
      repoDir.toFile().deleteRecursively()
    }
  }

  private class TestEnvironment : Environment {
    override fun <T> service(type: Class<T>): T = throw IllegalArgumentException("Unsupported service: ${type.name}")

    override fun publish(event: Any) = Unit
  }
}
