package io.github.architectplatform.api.core.tasks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FailureStrategyTest {
  @Test
  fun `ABORT is a singleton`() {
    assertSame(FailureStrategy.ABORT, FailureStrategy.ABORT)
  }

  @Test
  fun `CONTINUE is a singleton`() {
    assertSame(FailureStrategy.CONTINUE, FailureStrategy.CONTINUE)
  }

  @Test
  fun `RETRY holds maxAttempts`() {
    val retry = FailureStrategy.RETRY(3)
    assertEquals(3, retry.maxAttempts)
  }

  @Test
  fun `RETRY defaults to 1 attempt`() {
    val retry = FailureStrategy.RETRY()
    assertEquals(1, retry.maxAttempts)
  }

  @Test
  fun `RETRY rejects zero maxAttempts`() {
    assertThrows<IllegalArgumentException> {
      FailureStrategy.RETRY(0)
    }
  }

  @Test
  fun `RETRY rejects negative maxAttempts`() {
    assertThrows<IllegalArgumentException> {
      FailureStrategy.RETRY(-1)
    }
  }

  @Test
  fun `default Task onFailure is ABORT`() {
    val task =
      object : Task {
        override val id = "test"

        override fun execute(
          environment: Environment,
          projectContext: io.github.architectplatform.api.core.project.ProjectContext,
          args: List<String>,
        ) = TaskResult.success()
      }
    assertTrue(task.onFailure() is FailureStrategy.ABORT)
  }
}
