package io.github.architectplatform.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.cli.client.ExecutionId

/**
 * Console user interface for task execution with progress-tree rendering, batch grouping,
 * timing, failure detail capture, and an execution summary table.
 *
 * In rich mode (not plain, verbosity >= 1) delegates live rendering to
 * [ParallelProgressView] which shows animated spinners and collapses completed tasks.
 *
 * Supports two modes:
 * - Interactive: Output with ANSI colors + in-place parallel progress view
 * - Plain: Simple text output for CI environments (no colors, no in-place updates)
 *
 * @property taskName The name of the task being executed
 * @property plain If true, disables ANSI colors for CI environments
 */
class ConsoleUI(
  private val taskName: String,
  private val plain: Boolean = false,
  var verbosity: Int = 1,
  var timing: Boolean = false,
) {

  /**
   * ANSI color codes for terminal output formatting.
   */
  object AnsiColors {
    const val RESET = "\u001B[0m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val CYAN = "\u001B[36m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
  }

  // ── Rich parallel progress view ───────────────────────────────────────────
  // Used when not in plain mode and verbosity >= 1
  private val parallelView: ParallelProgressView? =
    if (!plain && verbosity >= 1) ParallelProgressView(plain = false, width = terminalWidth()) else null

  private fun terminalWidth(): Int =
    runCatching {
      val proc = ProcessBuilder("stty", "size")
        .redirectInput(java.io.File("/dev/tty"))
        .start()
      val out = proc.inputStream.bufferedReader().readText().trim()
      proc.waitFor()
      out.split(" ").getOrNull(1)?.toIntOrNull() ?: 80
    }.getOrElse { 80 }

  // ── Task state tracking ─────────────────────────────────────────

  enum class TaskStatus { RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED }

  data class TaskState(
      val taskId: String,
      val status: TaskStatus,
      val startTimeMs: Long = System.currentTimeMillis(),
      val endTimeMs: Long? = null,
      val message: String? = null,
      val errorDetails: String? = null,
      val batch: Int = 0,
  ) {
    val durationMs: Long
      get() = (endTimeMs ?: System.currentTimeMillis()) - startTimeMs
  }

  private val taskStates = linkedMapOf<String, TaskState>()
  private val executionStartMs = System.currentTimeMillis()
  private var currentBatch = 0
  private var batchTaskCount = 0

  private var currentProject: String? = null
  private var currentSubProject: String? = null
  private var currentTask: String? = null
  private var failed = false

  val hasFailed: Boolean
    get() = failed

  /**
   * Returns snapshot of all tracked task states (for testing / summary).
   */
  fun taskStates(): Map<String, TaskState> = taskStates.toMap()

  data class ExecutionEvent(
      val executionId: ExecutionId,
      val executionEventType: ExecutionEventType,
      val success: Boolean = true,
      val message: String? = null,
  )

  enum class ExecutionEventType {
    STARTED,
    UPDATED,
    COMPLETED,
    FAILED,
    SKIPPED,
    OUTPUT
  }

  data class ArchitectEvent(
      val id: ArchitectEventId,
      val event: Map<String, Any> = emptyMap(),
  )

  private val objectMapper = ObjectMapper().registerKotlinModule()

  /**
   * Applies color to text if not in plain mode.
   */
  internal fun colorize(text: String, color: String): String {
    return if (plain) text else "$color$text${AnsiColors.RESET}"
  }

  /**
   * Processes an execution event, updates internal state, and renders the progress line.
   */
  fun process(eventMap: Map<String, Any>) {
    val event = objectMapper.convertValue<ArchitectEvent>(eventMap)
    val executionEventType = event.event["executionEventType"] as? String
    val project = event.event["project"] as? String
    val taskId = event.event["taskId"] as? String
    val message = event.event["message"] as? String
    val errorDetails = event.event["errorDetails"] as? String
    val subProject = event.event["subProject"] as? String

    // Update project context
    if (subProject != null) {
      currentSubProject = project
    } else if (project != null) {
      currentProject = project
      currentSubProject = null
    }
    if (taskId != null) currentTask = taskId

    // ── Update task state tracking ─────────────────────────────
    if (taskId != null) {
      when (executionEventType) {
        "STARTED" -> {
          // Detect batch boundary: if all previous tasks in current batch are done, bump batch
          if (taskStates.isNotEmpty()) {
            val batchTasks = taskStates.values.filter { it.batch == currentBatch }
            if (batchTasks.isNotEmpty() && batchTasks.all { it.status != TaskStatus.RUNNING }) {
              currentBatch++
              batchTaskCount = 0
            }
          }
          batchTaskCount++
          taskStates[taskId] = TaskState(taskId, TaskStatus.RUNNING, batch = currentBatch, message = message)
        }
        "COMPLETED" -> {
          val prev = taskStates[taskId]
          taskStates[taskId] = (prev ?: TaskState(taskId, TaskStatus.COMPLETED, batch = currentBatch))
              .copy(status = TaskStatus.COMPLETED, endTimeMs = System.currentTimeMillis(), message = message)
        }
        "FAILED" -> {
          failed = true
          val prev = taskStates[taskId]
          taskStates[taskId] = (prev ?: TaskState(taskId, TaskStatus.FAILED, batch = currentBatch))
              .copy(status = TaskStatus.FAILED, endTimeMs = System.currentTimeMillis(), message = message, errorDetails = errorDetails)
        }
        "SKIPPED" -> {
          val prev = taskStates[taskId]
          taskStates[taskId] = (prev ?: TaskState(taskId, TaskStatus.SKIPPED, batch = currentBatch))
              .copy(status = TaskStatus.SKIPPED, endTimeMs = System.currentTimeMillis(), message = message)
        }
        "CANCELLED" -> {
          val prev = taskStates[taskId]
          taskStates[taskId] = (prev ?: TaskState(taskId, TaskStatus.CANCELLED, batch = currentBatch))
              .copy(status = TaskStatus.CANCELLED, endTimeMs = System.currentTimeMillis(), message = message)
        }
      }
    }

    // ── Render progress line ────────────────────────────────────────────
    // Route through ParallelProgressView for rich in-place rendering when available,
    // otherwise fall back to simple line-by-line output.
    if (parallelView != null && taskId != null) {
      val state = taskStates[taskId]
      val durationMs = state?.durationMs ?: 0L
      when (executionEventType) {
        "STARTED" -> if (verbosity >= 1) parallelView.taskStarted(taskId, message)
        "COMPLETED" -> if (verbosity >= 1) parallelView.taskCompleted(taskId, durationMs, message)
        "FAILED" -> parallelView.taskFailed(taskId, durationMs, errorDetails)
        "SKIPPED" -> if (verbosity >= 1) parallelView.taskSkipped(taskId, message)
        "CANCELLED" -> parallelView.taskCancelled(taskId, durationMs)
        "OUTPUT" -> if (verbosity >= 2) parallelView.output(message ?: "")
      }
      return
    }

    // ── Plain / fallback rendering (verbosity-gated) ──────────────────────
    // Level 0: only failures; Level 1: task names+durations; Level 2: +messages; Level 3: all
    val shouldPrint = when (executionEventType) {
      "FAILED", "CANCELLED" -> true // Always show failures and cancellations
      "STARTED", "COMPLETED", "SKIPPED" -> verbosity >= 1
      "OUTPUT" -> verbosity >= 2
      else -> verbosity >= 3
    }

    if (shouldPrint) {
      val icon = when (executionEventType) {
        "STARTED" -> "▶"
        "COMPLETED" -> "✓"
        "FAILED" -> "✗"
        "CANCELLED" -> "⊘"
        "SKIPPED" -> "⏭"
        "OUTPUT" -> "│"
        else -> "·"
      }

      val statusColor = when (executionEventType) {
        "STARTED" -> AnsiColors.CYAN
        "COMPLETED" -> AnsiColors.GREEN
        "FAILED" -> AnsiColors.RED
        "CANCELLED" -> AnsiColors.YELLOW
        "SKIPPED" -> AnsiColors.YELLOW
        else -> ""
      }

      val elapsed = if (taskId != null) {
        val state = taskStates[taskId]
        if (state != null) " ${colorize(formatDuration(state.durationMs), AnsiColors.DIM)}" else ""
      } else ""

      val projectContext = buildString {
        if (subProject != null) {
          append(colorize(subProject, AnsiColors.CYAN))
          project?.let { append(" → ${colorize(it, AnsiColors.YELLOW)}") }
        } else {
          currentProject?.let { append(colorize(it, AnsiColors.CYAN)) }
        }
      }

      val parts = mutableListOf<String>()
      parts.add(colorize("$icon ${executionEventType ?: "EVENT"}", statusColor))
      if (projectContext.isNotEmpty()) parts.add("[${projectContext}]")
      if (taskId != null) parts.add(colorize(taskId, AnsiColors.BOLD))
      if (verbosity >= 2) message?.let { parts.add("- $it") }
      parts.add(elapsed)

      println(parts.joinToString(" ").trimEnd())
    }

    // ── Failure details (always shown inline in plain mode) ───────────────
    if (!errorDetails.isNullOrEmpty()) {
      println()
      println(colorize("  FAILURE DETAILS ($taskId):", "${AnsiColors.BOLD}${AnsiColors.RED}"))
      println(colorize("  ${"─".repeat(76)}", AnsiColors.RED))
      errorDetails.lines().forEach { line ->
        println(colorize("  $line", AnsiColors.RED))
      }
      println(colorize("  ${"─".repeat(76)}", AnsiColors.RED))
      println()
    }
  }

  /**
   * Prints the execution summary table and overall result.
   */
  fun printSummary() {
    // Finish the live progress view before printing the summary
    parallelView?.finish()
    if (taskStates.isEmpty()) return
    println()
    println("━".repeat(80))
    println(colorize("  Execution Summary", AnsiColors.BOLD))
    println("━".repeat(80))

    val fmt = "  %-6s  %-30s  %-10s  %s"
    println(fmt.format("STATUS", "TASK", "DURATION", "MESSAGE"))
    println("  ${"─".repeat(76)}")

    for ((_, state) in taskStates) {
      val statusIcon = when (state.status) {
        TaskStatus.COMPLETED -> colorize("✓", AnsiColors.GREEN)
        TaskStatus.FAILED -> colorize("✗", AnsiColors.RED)
        TaskStatus.SKIPPED -> colorize("⏭", AnsiColors.YELLOW)
        TaskStatus.CANCELLED -> colorize("⊘", AnsiColors.YELLOW)
        TaskStatus.RUNNING -> colorize("…", AnsiColors.CYAN)
      }
      val duration = formatDuration(state.durationMs)
      val msg = state.message?.take(40) ?: ""
      println(fmt.format(statusIcon, state.taskId.take(30), duration, msg))
    }

    val totalDuration = System.currentTimeMillis() - executionStartMs
    println("  ${"─".repeat(76)}")
    val taskCount = taskStates.size
    val failedCount = taskStates.values.count { it.status == TaskStatus.FAILED }
    val skippedCount = taskStates.values.count { it.status == TaskStatus.SKIPPED }
    val cancelledCount = taskStates.values.count { it.status == TaskStatus.CANCELLED }
    val successCount = taskStates.values.count { it.status == TaskStatus.COMPLETED }

    val summary = buildString {
      append("  $taskCount task(s): ")
      append(colorize("$successCount passed", AnsiColors.GREEN))
      if (failedCount > 0) append(", ${colorize("$failedCount failed", AnsiColors.RED)}")
      if (cancelledCount > 0) append(", ${colorize("$cancelledCount cancelled", AnsiColors.YELLOW)}")
      if (skippedCount > 0) append(", ${colorize("$skippedCount skipped", AnsiColors.YELLOW)}")
      append("  Total: ${formatDuration(totalDuration)}")
    }
    println(summary)
    println()

    // Print full failure details at the end
    val failedTasks = taskStates.values.filter { it.status == TaskStatus.FAILED && !it.errorDetails.isNullOrEmpty() }
    if (failedTasks.isNotEmpty()) {
      println(colorize("  Failed task details:", "${AnsiColors.BOLD}${AnsiColors.RED}"))
      println()
      for (ft in failedTasks) {
        println(colorize("  ✗ ${ft.taskId}", "${AnsiColors.BOLD}${AnsiColors.RED}"))
        println(colorize("  ${"─".repeat(76)}", AnsiColors.RED))
        ft.errorDetails?.lines()?.forEach { line ->
          println(colorize("  $line", AnsiColors.RED))
        }
        println(colorize("  ${"─".repeat(76)}", AnsiColors.RED))
        println()
      }
    }
  }

  /**
   * Marks the execution as complete with a success message.
   */
  fun complete(finalMessage: String) {
    printSummary()
    if (timing) printTimingWaterfall()
    println(colorize("✓ $finalMessage", "${AnsiColors.BOLD}${AnsiColors.GREEN}"))
  }

  /**
   * Marks the execution as failed with an error message.
   */
  fun completeWithError(errorMessage: String) {
    printSummary()
    if (timing) printTimingWaterfall()
    println(colorize("✗ $errorMessage", "${AnsiColors.BOLD}${AnsiColors.RED}"))
    failed = true
  }

  /**
   * Prints an ASCII waterfall timeline showing parallel execution and durations.
   */
  fun printTimingWaterfall() {
    if (taskStates.isEmpty()) return
    val totalMs = System.currentTimeMillis() - executionStartMs
    if (totalMs <= 0) return

    println()
    println(colorize("  ⏱ Timing Waterfall", AnsiColors.BOLD))
    println("  ${"─".repeat(76)}")

    val barWidth = 50
    for ((_, state) in taskStates) {
      val offsetMs = state.startTimeMs - executionStartMs
      val startFraction = (offsetMs.toDouble() / totalMs).coerceIn(0.0, 1.0)
      val durationFraction = (state.durationMs.toDouble() / totalMs).coerceIn(0.0, 1.0 - startFraction)

      val startPos = (startFraction * barWidth).toInt()
      val barLen = (durationFraction * barWidth).toInt().coerceAtLeast(1)

      val barChar = when (state.status) {
        TaskStatus.COMPLETED -> "█"
        TaskStatus.FAILED -> "▓"
        TaskStatus.CANCELLED -> "░"
        TaskStatus.SKIPPED -> "·"
        TaskStatus.RUNNING -> "▒"
      }
      val barColor = when (state.status) {
        TaskStatus.COMPLETED -> AnsiColors.GREEN
        TaskStatus.FAILED -> AnsiColors.RED
        TaskStatus.CANCELLED -> AnsiColors.YELLOW
        TaskStatus.SKIPPED -> AnsiColors.DIM
        TaskStatus.RUNNING -> AnsiColors.CYAN
      }

      val bar = " ".repeat(startPos) + barChar.repeat(barLen)
      val taskLabel = state.taskId.take(20).padEnd(20)
      val duration = formatDuration(state.durationMs)

      println("  $taskLabel │${colorize(bar.padEnd(barWidth), barColor)}│ $duration")
    }

    println("  ${"─".repeat(76)}")
    println("  ${"".padEnd(20)} │${"0".padEnd(barWidth / 2)}${formatDuration(totalMs).padStart(barWidth / 2)}│")
    println()
  }

  companion object {
    fun formatDuration(ms: Long): String {
      return when {
        ms < 1000 -> "${ms}ms"
        ms < 60_000 -> "${"%.1f".format(ms / 1000.0)}s"
        else -> {
          val mins = ms / 60_000
          val secs = (ms % 60_000) / 1000
          "${mins}m ${secs}s"
        }
      }
    }
  }
}

typealias ArchitectEventId = String
