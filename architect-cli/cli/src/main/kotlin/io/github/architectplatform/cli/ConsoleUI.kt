package io.github.architectplatform.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.cli.client.ExecutionId

/**
 * Simple console user interface for task execution.
 *
 * Provides straightforward output showing:
 * - Current project and task being executed
 * - Events as they happen in real-time
 * - Error details with full stack traces
 *
 * Supports two modes:
 * - Interactive: Output with ANSI colors
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
    const val CYAN = "\u001B[36m"
    const val BOLD = "\u001B[1m"
  }

  private var currentProject: String? = null
  private var currentSubProject: String? = null
  private var currentTask: String? = null
  private var failed = false

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
  private fun colorize(text: String, color: String): String {
    return if (plain) text else "$color$text${AnsiColors.RESET}"
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
    if (project != null) currentProject = project
    if (subProject != null) currentSubProject = subProject
    if (taskId != null) currentTask = taskId

    // Determine icon and handle failures
    val icon = when (executionEventType) {
      "STARTED" -> "▶️"
      "COMPLETED" -> "✅"
      "FAILED" -> {
        failed = true
        "❌"
      }
      "SKIPPED" -> "⏭️"
      "OUTPUT" -> "📝"
      else -> "ℹ️"
    }

    // Build the output line
    val parts = mutableListOf<String>()
    
    // Event type
    parts.add("$icon $executionEventType")
    
    // Project context
    val projectContext = buildString {
      currentProject?.let { append(colorize(it, AnsiColors.CYAN)) }
      currentSubProject?.let { append(" → ${colorize(it, AnsiColors.YELLOW)}") }
    }
    if (projectContext.isNotEmpty()) {
      parts.add("[${projectContext}]")
    }
    
    // Task context
    currentTask?.let {
      parts.add("${colorize("Task:", AnsiColors.BOLD)} $it")
    }
    
    // Message
    message?.let {
      parts.add(it)
    }
    
    println(parts.joinToString(" "))
    
    // Display error details immediately if present
    if (!errorDetails.isNullOrEmpty()) {
      println()
      println(colorize("ERROR DETAILS:", "${AnsiColors.BOLD}${AnsiColors.RED}"))
      println(colorize("─".repeat(80), AnsiColors.RED))
      errorDetails.lines().forEach { line ->
        println(colorize(line, AnsiColors.RED))
      }
      println(colorize("─".repeat(80), AnsiColors.RED))
      println()
    }
  }

  /**
   * Marks the execution as complete with a success message.
   *
   * @param finalMessage Success message to display
   */
  fun complete(finalMessage: String) {
    println()
    println(colorize("✅ $finalMessage", "${AnsiColors.BOLD}${AnsiColors.GREEN}"))
  }

  /**
   * Marks the execution as failed with an error message.
   *
   * @param errorMessage Error message to display
   */
  fun completeWithError(errorMessage: String) {
    println()
    println(colorize("❌ $errorMessage", "${AnsiColors.BOLD}${AnsiColors.RED}"))
    failed = true
  }
}

typealias ArchitectEventId = String
