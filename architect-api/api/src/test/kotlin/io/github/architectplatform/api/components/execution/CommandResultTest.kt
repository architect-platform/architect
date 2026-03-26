package io.github.architectplatform.api.components.execution

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommandResultTest {
  @Test
  fun `successful command has success true`() {
    val result = CommandResult(exitCode = 0, stdout = "output", stderr = "", durationMs = 100)

    assertTrue(result.success)
    assertEquals(0, result.exitCode)
    assertEquals("output", result.stdout)
    assertEquals("", result.stderr)
    assertEquals(100, result.durationMs)
  }

  @Test
  fun `failed command has success false`() {
    val result = CommandResult(exitCode = 1, stdout = "", stderr = "error message", durationMs = 50)

    assertFalse(result.success)
    assertEquals(1, result.exitCode)
    assertEquals("error message", result.stderr)
  }

  @Test
  fun `defaults for stderr and durationMs`() {
    val result = CommandResult(exitCode = 0, stdout = "hello")

    assertEquals("", result.stderr)
    assertEquals(0, result.durationMs)
  }

  @Test
  fun `non-zero exit codes are all failures`() {
    assertFalse(CommandResult(exitCode = 1, stdout = "").success)
    assertFalse(CommandResult(exitCode = 127, stdout = "").success)
    assertFalse(CommandResult(exitCode = -1, stdout = "").success)
  }

  @Test
  fun `data class equality works correctly`() {
    val r1 = CommandResult(exitCode = 0, stdout = "ok", stderr = "", durationMs = 100)
    val r2 = CommandResult(exitCode = 0, stdout = "ok", stderr = "", durationMs = 100)

    assertEquals(r1, r2)
  }

  @Test
  fun `different exit codes are not equal`() {
    val r1 = CommandResult(exitCode = 0, stdout = "ok")
    val r2 = CommandResult(exitCode = 1, stdout = "ok")

    assertNotEquals(r1, r2)
  }
}
