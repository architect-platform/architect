package io.github.architectplatform.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.cli.client.ExecutionId

/**
 * Enhanced console user interface for task execution with improved formatting.
 *
 * Features:
 * - Structured output with clear sections
 * - Progress indicators showing task completion
 * - Better error display with clear formatting
 * - Execution summary with statistics
 * - Support for both interactive and plain modes
 *
 * Supports two modes:
 * - Interactive: Rich output with ANSI colors and formatting
 * - Plain: Simple text output for CI environments (no colors)
 *
 * @property taskName The name of the task being executed
 * @property plain If true, disables ANSI colors for CI environments
 */
class ConsoleUI(private val taskName: String, private val plain: Boolean = false) {

  /**
   * ANSI color codes for terminal output formatting.
   */
  object AnsiColors {
    const val RESET = "\u001B[0m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val MAGENTA = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
  }

  companion object {
    private const val SUMMARY_BOX_WIDTH = 79
    private const val SEPARATOR_WIDTH = 80
  }

  private var currentProject: String? = null
  private var currentSubProject: String? = null
  private var currentTask: String? = null
  private var failed = false
  
  // Statistics
  private var totalTasks = 0
  private var completedTasks = 0
  private var failedTasks = 0
  private var skippedTasks = 0
  private val taskTimes = mutableMapOf<String, Long>()
  private val taskStartTimes = mutableMapOf<String, Long>()

  /**
   * Indicates whether the task execution has failed.
   */
  val hasFailed: Boolean
    get() = failed


  /**
   * Represents an execution event received from the engine.
   */
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
    OUTPUT,
    TASK_COMPLETED
  }

  data class ArchitectEvent(
      val id: ArchitectEventId,
      val event: Map<String, Any> = emptyMap(),
  )

  private val objectMapper = ObjectMapper().registerKotlinModule()

  /**
   * Applies color to text if not in plain mode.
   */
  private fun colorize(text: String, color: String): String {
    return if (plain) text else "$color$text${AnsiColors.RESET}"
  }

  /**
   * Print a separator line.
   */
  private fun printSeparator(char: String = "─", color: String = AnsiColors.DIM) {
    println(colorize(char.repeat(SEPARATOR_WIDTH), color))
  }

  /**
   * Print a section header.
   */
  private fun printSection(title: String) {
    println()
    println(colorize("┌─ $title", "${AnsiColors.BOLD}${AnsiColors.CYAN}"))
  }

  /**
   * Print a section footer.
   */
  private fun printSectionEnd() {
    println(colorize("└─", "${AnsiColors.DIM}${AnsiColors.CYAN}"))
  }

  /**
   * Processes an execution event and displays it.
   *
   * @param eventMap Raw event data from the engine
   */
  fun process(eventMap: Map<String, Any>) {
    val event = objectMapper.convertValue<ArchitectEvent>(eventMap)
    val executionEventType = event.event["executionEventType"] as? String
    val project = event.event["project"] as? String
    val taskId = event.event["taskId"] as? String
    val message = event.event["message"] as? String
    val errorDetails = event.event["errorDetails"] as? String
    val subProject = event.event["subProject"] as? String

    // Update current state
    if (subProject != null) {
      currentSubProject = project
    } else if (project != null) {
      currentProject = project
      currentSubProject = null
    }
    
    if (taskId != null) currentTask = taskId

    // Process different event types
    when (executionEventType) {
      "STARTED" -> {
        if (taskId != null) {
          totalTasks++
          taskStartTimes[taskId] = System.currentTimeMillis()
          printTaskStart(project, taskId, message)
        } else {
          printSection("Execution Started")
          project?.let { println(colorize("│  Project: $it", AnsiColors.CYAN)) }
          message?.let { println(colorize("│  $it", AnsiColors.DIM)) }
        }
      }
      "COMPLETED" -> {
        if (taskId != null) {
          completedTasks++
          taskStartTimes[taskId]?.let { startTime ->
            taskTimes[taskId] = System.currentTimeMillis() - startTime
          }
          printTaskComplete(project, taskId, message)
        } else {
          // Execution completed - we'll show summary in complete()
        }
      }
      "FAILED" -> {
        if (taskId != null) {
          failedTasks++
          failed = true
          printTaskFailed(project, subProject, taskId, message, errorDetails)
        } else {
          failed = true
        }
      }
      "TASK_COMPLETED" -> {
        completedTasks++
        taskStartTimes[taskId]?.let { startTime ->
          taskTimes[taskId!!] = System.currentTimeMillis() - startTime
        }
        printTaskComplete(project, taskId, message)
      }
      "SKIPPED" -> {
        skippedTasks++
        printTaskSkipped(project, taskId, message)
      }
      "OUTPUT" -> {
        message?.let { printTaskOutput(it) }
      }
      else -> {
        // Unknown event type, print as info
        println(colorize("│  ℹ️  $executionEventType", AnsiColors.DIM))
        message?.let { println(colorize("│     $it", AnsiColors.DIM)) }
      }
    }
  }

  private fun printTaskStart(project: String?, taskId: String?, message: String?) {
    val projectDisplay = project?.let { colorize(it, AnsiColors.CYAN) } ?: ""
    val taskDisplay = taskId?.let { colorize(it, AnsiColors.WHITE) } ?: ""
    println(colorize("│  ▶️  ", AnsiColors.BLUE) + "Starting: $projectDisplay:$taskDisplay")
    message?.let { 
      println(colorize("│     $it", AnsiColors.DIM)) 
    }
  }

  private fun printTaskComplete(project: String?, taskId: String?, message: String?) {
    val projectDisplay = project?.let { colorize(it, AnsiColors.CYAN) } ?: ""
    val taskDisplay = taskId?.let { colorize(it, AnsiColors.WHITE) } ?: ""
    val duration = taskId?.let { taskTimes[it]?.let { time -> " (${formatDuration(time)})" } } ?: ""
    println(colorize("│  ✅ ", AnsiColors.GREEN) + "Completed: $projectDisplay:$taskDisplay${colorize(duration, AnsiColors.DIM)}")
    message?.let { 
      println(colorize("│     $it", AnsiColors.DIM)) 
    }
  }

  private fun printTaskFailed(project: String?, subProject: String?, taskId: String?, message: String?, errorDetails: String?) {
    val projectDisplay = if (subProject != null) {
      // This is a subproject event: show subProject → project
      "${colorize(subProject, AnsiColors.CYAN)} → ${colorize(project ?: "", AnsiColors.YELLOW)}"
    } else {
      project?.let { colorize(it, AnsiColors.CYAN) } ?: ""
    }
    val taskDisplay = taskId?.let { colorize(it, AnsiColors.WHITE) } ?: ""
    println(colorize("│  ❌ ", AnsiColors.RED) + "Failed: $projectDisplay:$taskDisplay")
    message?.let { 
      println(colorize("│     $it", AnsiColors.RED)) 
    }
    
    if (!errorDetails.isNullOrEmpty()) {
      println(colorize("│", AnsiColors.DIM))
      println(colorize("│  ╭─ Error Details", "${AnsiColors.BOLD}${AnsiColors.RED}"))
      errorDetails.lines().forEach { line ->
        println(colorize("│  │ ", AnsiColors.RED) + colorize(line, AnsiColors.RED))
      }
      println(colorize("│  ╰─", AnsiColors.RED))
    }
  }

  private fun printTaskSkipped(project: String?, taskId: String?, message: String?) {
    val projectDisplay = project?.let { colorize(it, AnsiColors.CYAN) } ?: ""
    val taskDisplay = taskId?.let { colorize(it, AnsiColors.WHITE) } ?: ""
    println(colorize("│  ⏭️  ", AnsiColors.YELLOW) + "Skipped: $projectDisplay:$taskDisplay")
    message?.let { 
      println(colorize("│     $it", AnsiColors.DIM)) 
    }
  }

  private fun printTaskOutput(output: String) {
    output.lines().forEach { line ->
      println(colorize("│  ", AnsiColors.DIM) + line)
    }
  }

  private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000.0
    return when {
      seconds < 1.0 -> "${millis}ms"
      seconds < 60.0 -> "${"%.2f".format(seconds)}s"
      else -> {
        val minutes = (seconds / 60).toInt()
        val remainingSeconds = seconds % 60
        "${minutes}m ${"%.0f".format(remainingSeconds)}s"
      }
    }
  }

  /**
   * Marks the execution as complete with a success message and displays summary.
   *
   * @param finalMessage Success message to display
   */
  fun complete(finalMessage: String) {
    printSectionEnd()
    println()
    printSummary(success = true)
    println()
    println(colorize("✅ $finalMessage", "${AnsiColors.BOLD}${AnsiColors.GREEN}"))
  }

  /**
   * Marks the execution as failed with an error message and displays summary.
   *
   * @param errorMessage Error message to display
   */
  fun completeWithError(errorMessage: String) {
    printSectionEnd()
    println()
    printSummary(success = false)
    println()
    println(colorize("❌ $errorMessage", "${AnsiColors.BOLD}${AnsiColors.RED}"))
    failed = true
  }

  /**
   * Print execution summary with statistics.
   */
  private fun printSummary(success: Boolean) {
    println(colorize("╔═══════════════════════════════════════════════════════════════════════════════╗", AnsiColors.BOLD))
    println(colorize("║", AnsiColors.BOLD) + colorize(" EXECUTION SUMMARY".padEnd(SUMMARY_BOX_WIDTH), "${AnsiColors.BOLD}${AnsiColors.WHITE}") + colorize("║", AnsiColors.BOLD))
    println(colorize("╠═══════════════════════════════════════════════════════════════════════════════╣", AnsiColors.BOLD))
    
    // Status
    val statusIcon = if (success) "✅" else "❌"
    val statusText = if (success) "SUCCESS" else "FAILED"
    val statusColor = if (success) AnsiColors.GREEN else AnsiColors.RED
    println(colorize("║", AnsiColors.BOLD) + "  Status: $statusIcon ${colorize(statusText, "${AnsiColors.BOLD}$statusColor")}".padEnd(90) + colorize("║", AnsiColors.BOLD))
    
    // Task statistics
    println(colorize("║", AnsiColors.BOLD) + "  Tasks:  Total: ${colorize(totalTasks.toString(), AnsiColors.BOLD)}".padEnd(90) + colorize("║", AnsiColors.BOLD))
    
    if (completedTasks > 0) {
      println(colorize("║", AnsiColors.BOLD) + "          Completed: ${colorize(completedTasks.toString(), AnsiColors.GREEN)}".padEnd(90) + colorize("║", AnsiColors.BOLD))
    }
    if (failedTasks > 0) {
      println(colorize("║", AnsiColors.BOLD) + "          Failed: ${colorize(failedTasks.toString(), AnsiColors.RED)}".padEnd(90) + colorize("║", AnsiColors.BOLD))
    }
    if (skippedTasks > 0) {
      println(colorize("║", AnsiColors.BOLD) + "          Skipped: ${colorize(skippedTasks.toString(), AnsiColors.YELLOW)}".padEnd(90) + colorize("║", AnsiColors.BOLD))
    }
    
    println(colorize("╚═══════════════════════════════════════════════════════════════════════════════╝", AnsiColors.BOLD))
  }
}

typealias ArchitectEventId = String
