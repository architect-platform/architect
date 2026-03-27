package io.github.architectplatform.cli

import io.github.architectplatform.cli.dto.TaskDTO
import java.io.File
import java.io.FileInputStream

/**
 * Interactive fuzzy task selector for the Architect CLI.
 *
 * When invoked with no arguments, presents an arrow-key-navigable,
 * fuzzy-searchable list of tasks grouped by phase. Falls back to plain
 * list rendering in CI / non-interactive environments.
 *
 * Key bindings:
 *  ↑ / ↓      — navigate task list
 *  Any char   — append to search filter
 *  Backspace  — remove last filter character
 *  Enter      — execute highlighted task
 *  Esc / q    — quit without executing
 *  Ctrl+C     — abort
 */
class InteractiveTaskSelector(private val plain: Boolean = false) {

  // ── ANSI helpers ─────────────────────────────────────────────────────────

  private val reset = "\u001B[0m"
  private val bold = "\u001B[1m"
  private val dim = "\u001B[2m"
  private val cyan = "\u001B[36m"
  private val yellow = "\u001B[33m"
  private val green = "\u001B[32m"
  private val white = "\u001B[37m"
  private val bgSelected = "\u001B[48;5;24m"  // dark-blue background for selection

  private fun c(text: String, vararg codes: String): String =
    if (plain) text else codes.joinToString("") + text + reset

  // ── Sealed key types ─────────────────────────────────────────────────────

  private sealed class Key {
    object Up : Key()
    object Down : Key()
    object Enter : Key()
    object Backspace : Key()
    object Escape : Key()
    object CtrlC : Key()
    object Quit : Key()
    data class Char(val ch: kotlin.Char) : Key()
    object Other : Key()
  }

  // ── Selectable item (task or phase header) ───────────────────────────────

  private sealed class Item {
    data class Header(val phase: String) : Item()
    data class Task(val dto: TaskDTO) : Item()
  }

  // ── Public entry point ───────────────────────────────────────────────────

  /**
   * Displays the interactive selector and returns the selected task ID,
   * or `null` if the user cancelled or the environment is non-interactive.
   */
  fun select(tasks: List<TaskDTO>): String? {
    if (plain || tasks.isEmpty() || !isInteractive()) return null

    val savedSettings = saveTerminalSettings() ?: return null

    val ttyFile = File("/dev/tty")
    if (!ttyFile.exists()) return null

    setRawMode()
    val ttyIn = try { FileInputStream(ttyFile) } catch (_: Exception) { restoreTerminalSettings(savedSettings); return null }

    // Use alternate screen buffer to preserve the caller's terminal output
    print("\u001B[?1049h")
    print("\u001B[?25l")   // hide cursor
    System.out.flush()

    var searchQuery = ""
    var cursor = 0

    try {
      while (true) {
        val allItems = buildItems(tasks, searchQuery)
        val selectableIndices = allItems.indices.filter { allItems[it] is Item.Task }

        // Clamp cursor to valid selectable range
        if (selectableIndices.isEmpty()) {
          cursor = 0
        } else {
          if (cursor >= selectableIndices.size) cursor = selectableIndices.size - 1
          if (cursor < 0) cursor = 0
        }

        render(allItems, selectableIndices, cursor, searchQuery, tasks.size)

        val key = readKey(ttyIn)
        when (key) {
          Key.Up -> if (cursor > 0) cursor--
          Key.Down -> if (cursor < selectableIndices.size - 1) cursor++
          Key.Enter -> {
            if (selectableIndices.isNotEmpty()) {
              val selectedItem = allItems[selectableIndices[cursor]] as? Item.Task
              return selectedItem?.dto?.id
            }
          }
          Key.Backspace -> if (searchQuery.isNotEmpty()) {
            searchQuery = searchQuery.dropLast(1)
            cursor = 0
          }
          Key.Escape, Key.Quit, Key.CtrlC -> return null
          is Key.Char -> {
            searchQuery += key.ch
            cursor = 0
          }
          Key.Other -> Unit
        }
      }
    } finally {
      ttyIn.close()
      print("\u001B[?25h")   // show cursor
      print("\u001B[?1049l") // restore main screen buffer
      System.out.flush()
      restoreTerminalSettings(savedSettings)
    }
  }

  // ── Rendering ─────────────────────────────────────────────────────────────

  private fun render(
    items: List<Item>,
    selectableIndices: List<Int>,
    cursor: Int,
    searchQuery: String,
    totalTasks: Int,
  ) {
    val sb = StringBuilder()
    // Move to top-left and clear screen
    sb.append("\u001B[H\u001B[2J")

    val width = terminalWidth()
    val divider = "─".repeat(width)

    // ── Header ──
    sb.append(c("  🧭  Architect — Select a task", bold, cyan)).append("\n")
    sb.append(c("  ↑↓ navigate   type to filter   Enter=run   Esc/q=quit", dim)).append("\n")
    sb.append(c(divider, dim)).append("\n")

    // ── Search box ──
    val filterLabel = c(" 🔍 Filter: ", bold) + searchQuery + c("_", dim)
    sb.append(filterLabel).append("\n")
    sb.append(c(divider, dim)).append("\n")

    if (selectableIndices.isEmpty()) {
      sb.append(c("  (no tasks match \"$searchQuery\")", dim, yellow)).append("\n")
    } else {
      // Render items
      var selectableCounter = 0
      for (item in items) {
        when (item) {
          is Item.Header -> {
            sb.append("\n")
            sb.append(c("  ${item.phase}", bold, yellow)).append("\n")
          }
          is Item.Task -> {
            val isSelected = selectableCounter == cursor
            val prefix = if (isSelected) c(" ▶ ", bold, green) else "   "
            val idPad = item.dto.id.padEnd(24).take(24)
            val desc = item.dto.description.take(width - 32)
            val row = "$prefix${c(idPad, if (isSelected) bold else dim)}  $desc"
            if (isSelected && !plain) {
              sb.append(bgSelected).append(row.padEnd(width)).append(reset)
            } else {
              sb.append(row)
            }
            sb.append("\n")
            selectableCounter++
          }
        }
      }
    }

    sb.append("\n")
    sb.append(c(divider, dim)).append("\n")
    val matchCount = selectableIndices.size
    sb.append(c("  $matchCount / $totalTasks tasks", dim))
    if (selectableIndices.isNotEmpty()) {
      val selectedTask = (items[selectableIndices[cursor]] as? Item.Task)?.dto
      if (selectedTask != null) {
        sb.append("   ")
        sb.append(c("→ ${selectedTask.id}", bold, cyan))
        if (selectedTask.phase != null) sb.append(c("  [${selectedTask.phase}]", dim))
      }
    }
    sb.append("\n")

    print(sb.toString())
    System.out.flush()
  }

  // ── Item construction with fuzzy filtering ───────────────────────────────

  private fun buildItems(tasks: List<TaskDTO>, query: String): List<Item> {
    val filtered = if (query.isEmpty()) {
      tasks
    } else {
      val q = query.lowercase()
      tasks.filter { t ->
        t.id.lowercase().contains(q) ||
          t.description.lowercase().contains(q) ||
          (t.phase?.lowercase()?.contains(q) == true)
      }
    }

    // Group by phase (null → "OTHER")
    val grouped = filtered.groupBy { it.phase ?: "OTHER" }
    val phaseOrder = listOf("INIT", "LINT", "VERIFY", "BUILD", "TEST", "RUN", "RELEASE", "PUBLISH", "OTHER")
    val orderedPhases = (phaseOrder.filter { it in grouped } + (grouped.keys - phaseOrder.toSet()).sorted())

    val items = mutableListOf<Item>()
    for (phase in orderedPhases) {
      val phaseTasks = grouped[phase] ?: continue
      items.add(Item.Header(phase))
      phaseTasks.sortedBy { it.id }.forEach { items.add(Item.Task(it)) }
    }
    return items
  }

  // ── Raw key reading ───────────────────────────────────────────────────────

  private fun readKey(ttyIn: FileInputStream): Key {
    val b = ttyIn.read()
    return when (b) {
      27 -> {
        // ESC or ANSI escape sequence
        Thread.sleep(20)  // give the rest of the sequence time to arrive
        val available = ttyIn.available()
        if (available >= 2) {
          val b2 = ttyIn.read()
          val b3 = ttyIn.read()
          // Drain any remaining bytes (e.g., mouse events)
          repeat(ttyIn.available()) { ttyIn.read() }
          if (b2 == '['.code) {
            when (b3) {
              'A'.code -> Key.Up
              'B'.code -> Key.Down
              else -> Key.Other
            }
          } else {
            Key.Escape
          }
        } else {
          Key.Escape
        }
      }
      13, 10 -> Key.Enter    // CR or LF
      127, 8 -> Key.Backspace
      3 -> Key.CtrlC          // Ctrl+C
      'q'.code -> Key.Quit
      -1 -> Key.CtrlC         // EOF
      else -> if (b in 32..126) Key.Char(b.toChar()) else Key.Other
    }
  }

  // ── Terminal state management ─────────────────────────────────────────────

  private fun isInteractive(): Boolean {
    if (System.getenv("CI") != null) return false
    if (System.getenv("TERM") == "dumb") return false
    return File("/dev/tty").exists()
  }

  private fun saveTerminalSettings(): String? {
    return try {
      val proc = ProcessBuilder("stty", "-g")
        .redirectInput(File("/dev/tty"))
        .start()
      val saved = proc.inputStream.bufferedReader().readText().trim()
      proc.waitFor()
      saved.ifEmpty { null }
    } catch (_: Exception) {
      null
    }
  }

  private fun setRawMode() {
    try {
      ProcessBuilder("stty", "raw", "-echo", "min", "1", "time", "0")
        .redirectInput(File("/dev/tty"))
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
        .waitFor()
    } catch (_: Exception) {}
  }

  private fun restoreTerminalSettings(saved: String) {
    try {
      ProcessBuilder("stty", saved)
        .redirectInput(File("/dev/tty"))
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
        .waitFor()
    } catch (_: Exception) {}
  }

  private fun terminalWidth(): Int {
    return try {
      val proc = ProcessBuilder("stty", "size")
        .redirectInput(File("/dev/tty"))
        .start()
      val out = proc.inputStream.bufferedReader().readText().trim()
      proc.waitFor()
      // "rows cols"
      out.split(" ").getOrNull(1)?.toIntOrNull() ?: 80
    } catch (_: Exception) {
      80
    }
  }
}
