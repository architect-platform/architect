import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.cli.TerminalUI
import io.github.architectplatform.cli.client.ExecutionId
import kotlin.system.exitProcess

/**
 * Rich console user interface for task execution visualization.
 *
 * Provides a sophisticated terminal UI with:
 * - Real-time task execution tracking
 * - Event logging with colored output
 * - Progress visualization
 * - Summary statistics
 * - Error reporting
 *
 * Supports two modes:
 * - Interactive: Full terminal UI with colors and formatting
 * - Plain: Simple text output for CI environments
 *
 * @property taskName The name of the task being executed
 * @property plain If true, uses plain text output instead of rich terminal UI
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
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"
    const val BOLD = "\u001B[1m"
    const val ORANGE = "\u001B[38;5;208m"
  }

  private val ansiRegex = Regex("\u001B\\[[0-9;]*m")

  /**
   * Calculates the visible length of a string, excluding ANSI color codes.
   */
  private fun String.visibleLength(): Int = this.replace(ansiRegex, "").length

  companion object {
    private const val TOTAL_WIDTH = 150
    private const val LEFT_PANEL_WIDTH = 35
    private val RIGHT_PANEL_WIDTH = TOTAL_WIDTH - LEFT_PANEL_WIDTH - 3

    private const val INFO_COL_COUNT = 4
    private const val INFO_COL_GUTTERS = INFO_COL_COUNT + 1 // 4 columns + 5 separators
    private val INFO_COL_WIDTH = (TOTAL_WIDTH - INFO_COL_GUTTERS) / INFO_COL_COUNT

    private val EVENT_ID_COL_WIDTH = 32
    private val EVENT_CONTENT_COL_WIDTH = RIGHT_PANEL_WIDTH - EVENT_ID_COL_WIDTH - 6
  }

  private val ui = TerminalUI(TOTAL_WIDTH)

  /**
   * Represents a logged event in the execution timeline.
   *
   * @property id Unique identifier for the event
   * @property icon Emoji icon representing the event type
   * @property message Detailed message describing the event
   */
  data class EventLog(val id: ArchitectEventId, val icon: String, val message: String)

  private val eventsLog = mutableListOf<EventLog>()
  private val failureReasons = mutableListOf<String>()
  private var executionId: String = "N/A"
  private var lastMessage: String? = null
  private var failed = false

  /**
   * Indicates whether the task execution has failed.
   */
  val hasFailed: Boolean
    get() = failed

  /**
   * Represents an execution event received from the engine.
   *
   * @property executionId Unique identifier for the execution
   * @property executionEventType Type of event (STARTED, UPDATED, COMPLETED, FAILED, SKIPPED)
   * @property success Indicates if the event represents a successful operation
   * @property message Optional message providing additional context
   */
  data class ExecutionEvent(
      val executionId: ExecutionId,
      val executionEventType: ExecutionEventType,
      val success: Boolean = true,
      val message: String? = null,
  )

  /**
   * Types of execution events that can occur during task execution.
   */
  enum class ExecutionEventType {
    STARTED,
    UPDATED,
    COMPLETED,
    FAILED,
    SKIPPED
  }

  /**
   * Represents an event from the Architect system.
   *
   * @property id Unique identifier for the event
   * @property event Map containing event data
   */
  data class ArchitectEvent(
      val id: ArchitectEventId,
      val event: Map<String, Any> = emptyMap(),
  )

  private val objectMapper = ObjectMapper().registerKotlinModule()

  /**
   * Processes an execution event and updates the UI.
   *
   * Converts the raw event map to an ArchitectEvent, determines the appropriate
   * icon based on event type, and triggers a UI redraw.
   *
   * @param eventMap Raw event data from the engine
   */
  fun process(eventMap: Map<String, Any>) {
    val event = objectMapper.convertValue<ArchitectEvent>(eventMap)
    val executionEventType = event.event["executionEventType"] as? String
    val icon = getIconForEventType(executionEventType, event)
    
    // Extract message and error details
    val message = event.event["message"] as? String
    val errorDetails = event.event["errorDetails"] as? String
    val subProject = event.event["subProject"] as? String
    
    // Build display message
    val displayMessage = buildString {
      append(objectMapper.writeValueAsString(event.event))
      if (message != null && message.isNotEmpty()) {
        append(" | Msg: $message")
      }
      if (subProject != null) {
        append(" | SubProject: $subProject")
      }
    }
    
    icon?.let { eventsLog.add(EventLog(event.id, it, displayMessage)) }
    
    // Store error details for display in the failure section
    if (errorDetails != null && errorDetails.isNotEmpty()) {
      failureReasons.add(errorDetails)
    }
    
    redraw()
  }

  /**
   * Determines the appropriate icon for an event type.
   *
   * @param eventType The type of event as a string
   * @param event The full event object
   * @return The emoji icon for this event type, or null if unknown
   */
  private fun getIconForEventType(eventType: String?, event: ArchitectEvent): String? {
    return eventType?.let { type ->
      when (type) {
        "STARTED" -> "▶️"
        "COMPLETED" -> "✅"
        "FAILED" -> {
          if (event.event["taskId"] == null) {
            failed = true
          }
          "❌"
        }
        "SKIPPED" -> "⏭️"
        "OUTPUT" -> "📝"
        else -> "ℹ️"
      }
    }
  }

  /**
   * Marks the execution as complete with a success message.
   *
   * @param finalMessage Success message to display
   */
  fun complete(finalMessage: String) {
    lastMessage = "✅  $finalMessage"
    redraw()
  }

  /**
   * Marks the execution as failed with an error message.
   *
   * @param errorMessage Error message to display
   */
  fun completeWithError(errorMessage: String) {
    lastMessage = "❌  Error: $errorMessage"
    failed = true
    redraw()
  }

  /**
   * Redraws the entire UI with current state.
   *
   * In plain mode, only prints the latest event. In interactive mode,
   * clears the screen and renders a full UI with:
   * - Header with project and task info
   * - Summary statistics
   * - Event log table
   * - Failure details (if any)
   */
  private fun redraw() {
    if (plain) {
      // In plain mode, show structured output for CI environments
      if (eventsLog.isNotEmpty()) {
        val lastEvent = eventsLog.last()
        println("[${lastEvent.id}] ${lastEvent.icon} ${lastEvent.message}")
      }
      return
    }
    print("\u001B[2J\u001B[H") // Clear screen

    ui.clear()
    ui.drawLine('╔', null, '╗', '═')
    ui.addCenteredLine("${AnsiColors.BOLD}${AnsiColors.CYAN}🚀 Architect CLI${AnsiColors.RESET}")
    ui.drawLine('╠', null, '╣', '═')

    val projectName = System.getProperty("user.dir").substringAfterLast('/')
    val infoCells =
        listOf(
            "📦 ${AnsiColors.CYAN}Project:${AnsiColors.RESET} $projectName",
            "🧪 ${AnsiColors.CYAN}Task:${AnsiColors.RESET} $taskName",
            "🧾 ${AnsiColors.CYAN}Exec:${AnsiColors.RESET} $executionId",
            "📊 ${AnsiColors.CYAN}Status:${AnsiColors.RESET} ${lastMessage ?: "..."}")

    val cells = infoCells.map { truncateOrPadAnsi(it, INFO_COL_WIDTH) }
    val content = cells.joinToString("│")
    val visible = content.replace(Regex("\u001B\\[[0-9;]*m"), "").length
    val padding = " ".repeat((TOTAL_WIDTH - 2 - visible).coerceAtLeast(0)) // exclude ║ ║
    ui.addLine("║$content$padding║")
    ui.drawLine('╠', null, '╣', '═')

    ui.addSplitLine(
        boldYellow(" 📈 Summary").padEnd(LEFT_PANEL_WIDTH - 1),
        boldYellow(" 🗂️ Tasks"),
        LEFT_PANEL_WIDTH,
        RIGHT_PANEL_WIDTH)

    val summaryLines =
        listOf(
            " 🔍 Event Seen    : ${eventsLog.size}",
            " 🚨 Failures      : ${if (failed) 1 else 0}",
            " 📊 Progress      : ${eventsLog.size} / ${eventsLog.size} tasks")

    val headers = listOf("🛠️ Event", "💬 Content")

    val rows = eventsLog.map { listOf(it.id, it.message) }

    val maxLines = maxOf(summaryLines.size, rows.size + 4)

    for (i in 0 until maxLines) {
      val left = if (i < summaryLines.size) summaryLines[i] else " ".repeat(LEFT_PANEL_WIDTH - 1)
      val right =
          when (i) {
            0 -> tableLine('┌', '┬', '┐', '─')
            1 -> tableRow(headers)
            2 -> tableLine('├', '┼', '┤', '─')
            in 3 until rows.size + 3 -> tableRow(rows[i - 3])
            rows.size + 3 -> tableLine('└', '┴', '┘', '─')
            else -> " ".repeat(RIGHT_PANEL_WIDTH - 1)
          }

      ui.addSplitLine(left, right, LEFT_PANEL_WIDTH, RIGHT_PANEL_WIDTH)
    }

    if (failureReasons.isNotEmpty()) {
      ui.drawLine('╠', null, '╣', '═')
      ui.addCenteredLine("${AnsiColors.BOLD}${AnsiColors.RED}❌  Failure Details${AnsiColors.RESET}")
      ui.drawLine('╠', null, '╣', '═')

      for (reason in failureReasons) {
        val reasonLines = wrapText(reason, TOTAL_WIDTH - 5)
        reasonLines.forEach { line ->
          val internalPadding = TOTAL_WIDTH - 5 - line.visibleLength()
          ui.addLine("║   $line${" ".repeat(internalPadding)}║")
        }
      }
    }

    // Draw this only once at the very end
    ui.drawLine('╚', null, '╝', '═')
    println(ui.render())
  }

  /**
   * Wraps text to fit within a specified width, preserving ANSI color codes.
   *
   * @param text Text to wrap
   * @param width Maximum line width
   * @return List of wrapped lines
   */
  private fun wrapText(text: String, width: Int): List<String> {
    val ansiRegex = Regex("\u001B\\[[0-9;]*m")
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()
    var visibleLength = 0

    for (word in words) {
      val stripped = ansiRegex.replace(word, "")
      val extraSpace = if (currentLine.isNotEmpty()) 1 else 0

      if (visibleLength + stripped.length + extraSpace > width) {
        lines += currentLine.toString()
        currentLine = StringBuilder()
        visibleLength = 0
      }

      if (currentLine.isNotEmpty()) {
        currentLine.append(" ")
        visibleLength += 1
      }

      currentLine.append(word)
      visibleLength += stripped.length
    }

    if (currentLine.isNotEmpty()) lines += currentLine.toString()
    return lines
  }

  private fun tableLine(left: Char, mid: Char, right: Char, fill: Char): String {
    val widths = listOf(EVENT_ID_COL_WIDTH, EVENT_CONTENT_COL_WIDTH)
    return buildString {
      append(left)
      for ((i, w) in widths.withIndex()) {
        append(fill.toString().repeat(w))
        append(if (i == widths.lastIndex) right else mid)
      }
    }
  }

  private fun tableRow(cells: List<String>): String {
    val widths = listOf(EVENT_ID_COL_WIDTH, EVENT_CONTENT_COL_WIDTH)
    return buildString {
      append('│')
      for ((i, cell) in cells.withIndex()) {
        append(truncateOrPadAnsi(cell, widths[i]))
        append('│')
      }
    }
  }

  private fun truncateOrPadAnsi(text: String, width: Int): String {
    val ansiRegex = Regex("\u001B\\[[0-9;]*m")
    val stripped = ansiRegex.replace(text, "")
    val visibleLength = stripped.length

    // Truncate if needed (ensure space for "…" and padding)
    val safeContent =
        if (visibleLength > width - 2) {
          stripped.take(width - 3) + "…"
        } else {
          stripped
        }

    val content = " $safeContent "
    val visibleWithPadding = content.length
    val padding = width - visibleWithPadding

    return content + " ".repeat(padding.coerceAtLeast(0))
  }

  private fun boldYellow(text: String): String =
      "${AnsiColors.BOLD}${AnsiColors.YELLOW}$text${AnsiColors.RESET}"
}

typealias ArchitectEventId = String
