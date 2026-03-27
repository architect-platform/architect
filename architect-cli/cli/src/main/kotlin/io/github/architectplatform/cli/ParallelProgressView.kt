package io.github.architectplatform.cli

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * In-place rich progress renderer for parallel task execution.
 *
 * Maintains a "live area" at the bottom of the terminal that shows all
 * currently-running tasks with animated spinners and elapsed timers.
 * When a task completes or fails, it is removed from the live area and
 * a permanent one-liner (or failure block) is printed above it.
 *
 * In plain / CI mode the renderer degrades to simple line-by-line output.
 *
 * @param plain  Disable ANSI output (CI mode)
 * @param width  Terminal column width for truncation
 */
class ParallelProgressView(
  private val plain: Boolean = false,
  private val width: Int = 80,
) {

  // ── ANSI helpers ─────────────────────────────────────────────────────────

  private val reset = "\u001B[0m"
  private val bold = "\u001B[1m"
  private val dim = "\u001B[2m"
  private val green = "\u001B[32m"
  private val red = "\u001B[31m"
  private val yellow = "\u001B[33m"
  private val cyan = "\u001B[36m"

  private fun c(text: String, vararg codes: String): String =
    if (plain) text else codes.joinToString("") + text + reset

  // ── Spinner ───────────────────────────────────────────────────────────────

  private val spinnerFrames = arrayOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
  @Volatile private var spinnerIdx = 0

  // ── Live task state ───────────────────────────────────────────────────────

  data class RunningTask(
    val id: String,
    val startMs: Long = System.currentTimeMillis(),
    val message: String? = null,
  )

  // All access to mutable state must be synchronized on `lock`
  private val lock = Any()
  private val runningTasks = linkedMapOf<String, RunningTask>()
  private var liveLines = 0  // how many lines the live area currently occupies

  // ── Background spinner thread ─────────────────────────────────────────────

  private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "architect-spinner").apply { isDaemon = true }
  }
  private var spinnerFuture: ScheduledFuture<*>? = null

  private fun startSpinner() {
    if (plain) return
    spinnerFuture = scheduler.scheduleAtFixedRate({
      synchronized(lock) {
        if (runningTasks.isNotEmpty()) {
          spinnerIdx = (spinnerIdx + 1) % spinnerFrames.size
          redrawLiveArea()
        }
      }
    }, 80, 80, TimeUnit.MILLISECONDS)
  }

  private fun stopSpinner() {
    spinnerFuture?.cancel(false)
    spinnerFuture = null
    scheduler.shutdown()
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /** Called when a task begins execution. */
  fun taskStarted(taskId: String, message: String? = null) {
    synchronized(lock) {
      if (runningTasks.isEmpty()) startSpinner()
      runningTasks[taskId] = RunningTask(taskId, message = message)
      if (!plain) redrawLiveArea()
      else println(c("  ▶ $taskId", cyan) + if (message != null) c("  $message", dim) else "")
    }
  }

  /** Called when a task completes successfully. */
  fun taskCompleted(taskId: String, durationMs: Long, message: String? = null) {
    synchronized(lock) {
      val wasRunning = runningTasks.remove(taskId) != null
      if (!plain) {
        clearLiveArea()
        val dur = c(ConsoleUI.formatDuration(durationMs), dim)
        val msg = if (message != null && message.isNotBlank()) c("  $message", dim) else ""
        println("  ${c("✓", bold, green)} ${c(taskId, bold)}  $dur$msg")
        redrawLiveArea()
      } else {
        println("  ✓ $taskId  ${ConsoleUI.formatDuration(durationMs)}")
      }
      if (runningTasks.isEmpty() && wasRunning) stopSpinner()
    }
  }

  /** Called when a task is skipped. */
  fun taskSkipped(taskId: String, reason: String? = null) {
    synchronized(lock) {
      runningTasks.remove(taskId)
      if (!plain) {
        clearLiveArea()
        val msg = if (reason != null) c("  $reason", dim) else ""
        println("  ${c("⏭", yellow)} ${c(taskId, dim)}$msg")
        redrawLiveArea()
      } else {
        println("  ⏭ $taskId${if (reason != null) "  $reason" else ""}")
      }
      if (runningTasks.isEmpty()) stopSpinner()
    }
  }

  /** Called when a task is cancelled. */
  fun taskCancelled(taskId: String, durationMs: Long) {
    synchronized(lock) {
      runningTasks.remove(taskId)
      if (!plain) {
        clearLiveArea()
        println("  ${c("⊘", yellow)} ${c(taskId, dim)}  ${c(ConsoleUI.formatDuration(durationMs), dim)}")
        redrawLiveArea()
      } else {
        println("  ⊘ $taskId  ${ConsoleUI.formatDuration(durationMs)}")
      }
      if (runningTasks.isEmpty()) stopSpinner()
    }
  }

  /**
   * Called when a task fails.
   *
   * Prints the failure header permanently and expands error details
   * (up to [maxErrorLines] lines) inline.
   */
  fun taskFailed(taskId: String, durationMs: Long, errorDetails: String? = null) {
    synchronized(lock) {
      runningTasks.remove(taskId)
      if (!plain) {
        clearLiveArea()
        println("  ${c("✗", bold, red)} ${c(taskId, bold, red)}  ${c(ConsoleUI.formatDuration(durationMs), dim)}")
        if (!errorDetails.isNullOrEmpty()) {
          println(c("  ${"─".repeat((width - 4).coerceAtLeast(20))}", dim, red))
          errorDetails.lines().take(20).forEach { line ->
            println(c("  $line", red))
          }
          println(c("  ${"─".repeat((width - 4).coerceAtLeast(20))}", dim, red))
        }
        redrawLiveArea()
      } else {
        println("  ✗ $taskId  ${ConsoleUI.formatDuration(durationMs)}")
        errorDetails?.lines()?.take(20)?.forEach { println("  $it") }
      }
      if (runningTasks.isEmpty()) stopSpinner()
    }
  }

  /** Emits an output line (verbosity >= 2). Does not affect live area. */
  fun output(line: String) {
    synchronized(lock) {
      if (!plain) {
        clearLiveArea()
        println(c("  │ $line", dim))
        redrawLiveArea()
      } else {
        println("  │ $line")
      }
    }
  }

  /** Clears the live area. Call before printing any permanent output. */
  fun finish() {
    synchronized(lock) {
      stopSpinner()
      clearLiveArea()
    }
  }

  // ── Internal rendering ────────────────────────────────────────────────────

  /** Erase the in-place live area (called before re-drawing or printing permanent lines). */
  private fun clearLiveArea() {
    if (plain || liveLines == 0) return
    // Move cursor up liveLines and clear to end of screen
    print("\u001B[${liveLines}A\u001B[J")
    System.out.flush()
    liveLines = 0
  }

  /** Render all currently-running tasks into the live area. */
  private fun redrawLiveArea() {
    if (plain || runningTasks.isEmpty()) return
    val frame = spinnerFrames[spinnerIdx]
    val sb = StringBuilder()
    for ((_, task) in runningTasks) {
      val elapsed = System.currentTimeMillis() - task.startMs
      val dur = ConsoleUI.formatDuration(elapsed)
      val idPad = task.id.take(width - 18)
      sb.append("  ${c(frame, cyan)} ${c(idPad, bold)}  ${c(dur, dim)}")
      task.message?.let { sb.append(c("  $it", dim).take(width - idPad.length - 12)) }
      sb.append("\n")
    }
    print(sb.toString())
    System.out.flush()
    liveLines = runningTasks.size
  }
}
