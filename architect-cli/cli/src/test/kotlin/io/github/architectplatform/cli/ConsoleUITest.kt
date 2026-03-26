package io.github.architectplatform.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Tests for [ConsoleUI] rendering: progress tree, timing, batch state,
 * summary table, failure details, and plain-mode color suppression.
 */
class ConsoleUITest {

  // ── Helpers ───────────────────────────────────────────────────────

  private fun captureOutput(block: () -> Unit): String {
    val buf = ByteArrayOutputStream()
    val original = System.out
    System.setOut(PrintStream(buf))
    try { block() } finally { System.setOut(original) }
    return buf.toString()
  }

  private fun event(
      taskId: String? = null,
      type: String = "STARTED",
      project: String = "test-project",
      message: String? = null,
      errorDetails: String? = null,
  ): Map<String, Any> {
    val inner = mutableMapOf<String, Any>(
        "executionEventType" to type,
        "project" to project,
    )
    taskId?.let { inner["taskId"] = it }
    message?.let { inner["message"] = it }
    errorDetails?.let { inner["errorDetails"] = it }
    return mapOf("id" to "test.event", "event" to inner)
  }

  // ── Task state tracking ───────────────────────────────────────────

  @Test
  fun `process STARTED event tracks task as RUNNING`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput { ui.process(event(taskId = "build", type = "STARTED")) }
    val states = ui.taskStates()
    assertEquals(ConsoleUI.TaskStatus.RUNNING, states["build"]?.status)
  }

  @Test
  fun `process COMPLETED event tracks task as COMPLETED`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "build", type = "STARTED"))
      ui.process(event(taskId = "build", type = "COMPLETED"))
    }
    val states = ui.taskStates()
    assertEquals(ConsoleUI.TaskStatus.COMPLETED, states["build"]?.status)
    assertTrue(states["build"]!!.durationMs >= 0)
  }

  @Test
  fun `process FAILED event sets hasFailed and tracks failure`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "test-task", type = "STARTED"))
      ui.process(event(taskId = "test-task", type = "FAILED", message = "oops"))
    }
    assertTrue(ui.hasFailed)
    assertEquals(ConsoleUI.TaskStatus.FAILED, ui.taskStates()["test-task"]?.status)
  }

  @Test
  fun `process SKIPPED event tracks task as SKIPPED`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "lint", type = "SKIPPED", message = "cached"))
    }
    assertEquals(ConsoleUI.TaskStatus.SKIPPED, ui.taskStates()["lint"]?.status)
  }

  // ── Rendering ─────────────────────────────────────────────────────

  @Test
  fun `progress line includes task id and event type`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 1)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED", message = "Starting"))
    }
    assertTrue(output.contains("STARTED"))
    assertTrue(output.contains("build"))
  }

  @Test
  fun `progress line includes elapsed time indicator`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 1)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED"))
      Thread.sleep(10)
      ui.process(event(taskId = "build", type = "COMPLETED"))
    }
    // Should contain ms or s time
    assertTrue(output.contains("ms") || output.contains("s"))
  }

  @Test
  fun `failure details are rendered inline`() {
    val ui = ConsoleUI("test", plain = true)
    val output = captureOutput {
      ui.process(event(taskId = "test-task", type = "FAILED", errorDetails = "NullPointerException at line 42"))
    }
    assertTrue(output.contains("FAILURE DETAILS"))
    assertTrue(output.contains("NullPointerException"))
  }

  // ── Summary ───────────────────────────────────────────────────────

  @Test
  fun `printSummary shows task count and status`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "a", type = "STARTED"))
      ui.process(event(taskId = "a", type = "COMPLETED"))
      ui.process(event(taskId = "b", type = "STARTED"))
      ui.process(event(taskId = "b", type = "FAILED"))
    }
    val output = captureOutput { ui.printSummary() }
    assertTrue(output.contains("Execution Summary"))
    assertTrue(output.contains("2 task(s)"))
    assertTrue(output.contains("1 passed"))
    assertTrue(output.contains("1 failed"))
  }

  @Test
  fun `complete calls printSummary and shows success`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "only", type = "STARTED"))
      ui.process(event(taskId = "only", type = "COMPLETED"))
    }
    val output = captureOutput { ui.complete("Done!") }
    assertTrue(output.contains("Done!"))
    assertTrue(output.contains("Execution Summary"))
  }

  @Test
  fun `completeWithError calls printSummary and shows error`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "fail", type = "STARTED"))
      ui.process(event(taskId = "fail", type = "FAILED", errorDetails = "bad"))
    }
    val output = captureOutput { ui.completeWithError("Task failed") }
    assertTrue(output.contains("Task failed"))
    assertTrue(output.contains("Execution Summary"))
    assertTrue(output.contains("Failed task details"))
  }

  // ── Plain mode ────────────────────────────────────────────────────

  @Test
  fun `plain mode suppresses ANSI codes`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 1)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED"))
    }
    assertFalse(output.contains("\u001B["))
  }

  @Test
  fun `interactive mode includes ANSI codes`() {
    val ui = ConsoleUI("test", plain = false, verbosity = 1)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED"))
    }
    assertTrue(output.contains("\u001B["))
  }

  // ── Duration formatting ───────────────────────────────────────────

  @Test
  fun `formatDuration formats milliseconds`() {
    assertEquals("50ms", ConsoleUI.formatDuration(50))
    assertEquals("999ms", ConsoleUI.formatDuration(999))
  }

  @Test
  fun `formatDuration formats seconds`() {
    assertEquals("1.0s", ConsoleUI.formatDuration(1000))
    assertEquals("5.5s", ConsoleUI.formatDuration(5500))
  }

  @Test
  fun `formatDuration formats minutes`() {
    assertEquals("1m 30s", ConsoleUI.formatDuration(90_000))
    assertEquals("2m 0s", ConsoleUI.formatDuration(120_000))
  }

  // ── Multiple tasks tracking ───────────────────────────────────────

  @Test
  fun `tracks multiple tasks independently`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "a", type = "STARTED"))
      ui.process(event(taskId = "b", type = "STARTED"))
      ui.process(event(taskId = "a", type = "COMPLETED"))
      ui.process(event(taskId = "b", type = "FAILED"))
    }
    val states = ui.taskStates()
    assertEquals(ConsoleUI.TaskStatus.COMPLETED, states["a"]?.status)
    assertEquals(ConsoleUI.TaskStatus.FAILED, states["b"]?.status)
    assertFalse(ui.taskStates().isEmpty())
  }

  @Test
  fun `hasFailed is false when all tasks succeed`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "x", type = "STARTED"))
      ui.process(event(taskId = "x", type = "COMPLETED"))
    }
    assertFalse(ui.hasFailed)
  }

  // ── Batch grouping ────────────────────────────────────────────────

  @Test
  fun `tasks started simultaneously are assigned to same batch`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "lint", type = "STARTED"))
      ui.process(event(taskId = "check", type = "STARTED"))
    }
    val states = ui.taskStates()
    assertEquals(states["lint"]?.batch, states["check"]?.batch)
  }

  @Test
  fun `batch boundary detected when all batch tasks complete before next starts`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      // Batch 0
      ui.process(event(taskId = "compile", type = "STARTED"))
      ui.process(event(taskId = "compile", type = "COMPLETED"))
      // Batch 1 — started after batch 0 all done
      ui.process(event(taskId = "test", type = "STARTED"))
    }
    val states = ui.taskStates()
    assertTrue(states["test"]!!.batch > states["compile"]!!.batch,
      "Expected test batch (${states["test"]?.batch}) > compile batch (${states["compile"]?.batch})")
  }

  @Test
  fun `batch does not advance while a task in current batch is still running`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "a", type = "STARTED"))
      ui.process(event(taskId = "b", type = "STARTED"))
      // Only 'a' completes, 'b' still running
      ui.process(event(taskId = "a", type = "COMPLETED"))
      // 'c' starts while 'b' is still running
      ui.process(event(taskId = "c", type = "STARTED"))
    }
    val states = ui.taskStates()
    // c should be in same batch as a/b since b hasn't finished
    assertEquals(states["a"]?.batch, states["c"]?.batch)
  }

  // ── Summary rendering ─────────────────────────────────────────────

  @Test
  fun `summary includes total duration`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "build", type = "STARTED"))
      ui.process(event(taskId = "build", type = "COMPLETED"))
    }
    val output = captureOutput { ui.printSummary() }
    assertTrue(output.contains("Total:"), "Expected 'Total:' in summary output")
  }

  @Test
  fun `summary shows skipped count when tasks are skipped`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "lint", type = "SKIPPED", message = "cached"))
      ui.process(event(taskId = "build", type = "STARTED"))
      ui.process(event(taskId = "build", type = "COMPLETED"))
    }
    val output = captureOutput { ui.printSummary() }
    assertTrue(output.contains("1 skipped"), "Expected '1 skipped' in summary output")
    assertTrue(output.contains("1 passed"), "Expected '1 passed' in summary output")
  }

  @Test
  fun `summary includes each task with status icon and duration`() {
    val ui = ConsoleUI("test", plain = true)
    captureOutput {
      ui.process(event(taskId = "compile", type = "STARTED"))
      ui.process(event(taskId = "compile", type = "COMPLETED"))
      ui.process(event(taskId = "test", type = "STARTED"))
      ui.process(event(taskId = "test", type = "FAILED", message = "assertion fail"))
    }
    val output = captureOutput { ui.printSummary() }
    assertTrue(output.contains("compile"))
    assertTrue(output.contains("test"))
    assertTrue(output.contains("ms") || output.contains("s"))
    assertTrue(output.contains("assertion fail"))
  }

  @Test
  fun `printSummary does nothing when no tasks processed`() {
    val ui = ConsoleUI("test", plain = true)
    val output = captureOutput { ui.printSummary() }
    assertEquals("", output)
  }

  // ── Verbosity levels ──────────────────────────────────────────────

  @Test
  fun `verbosity 0 suppresses non-failure events`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 0)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED"))
      ui.process(event(taskId = "build", type = "COMPLETED"))
    }
    assertFalse(output.contains("STARTED"))
    assertFalse(output.contains("COMPLETED"))
  }

  @Test
  fun `verbosity 0 still shows failures`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 0)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "FAILED", errorDetails = "boom"))
    }
    assertTrue(output.contains("FAILED"))
    assertTrue(output.contains("FAILURE DETAILS"))
  }

  @Test
  fun `verbosity 1 shows task events but not messages`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 1)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED", message = "Starting build"))
    }
    assertTrue(output.contains("STARTED"))
    assertTrue(output.contains("build"))
    assertFalse(output.contains("Starting build"))
  }

  @Test
  fun `verbosity 2 shows task events with messages`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 2)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "STARTED", message = "Starting build"))
    }
    assertTrue(output.contains("Starting build"))
  }

  @Test
  fun `verbosity 3 shows debug events`() {
    val ui = ConsoleUI("test", plain = true, verbosity = 3)
    val output = captureOutput {
      ui.process(event(taskId = "build", type = "UPDATED"))
    }
    assertTrue(output.contains("UPDATED"))
  }
}
