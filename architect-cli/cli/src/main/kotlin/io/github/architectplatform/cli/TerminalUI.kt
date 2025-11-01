package io.github.architectplatform.cli

/**
 * Low-level terminal rendering utilities for creating formatted text-based UIs.
 *
 * Provides building blocks for terminal output including:
 * - Bordered boxes and lines
 * - Split-panel layouts
 * - Centered text
 * - Table rendering
 * - ANSI color code handling
 *
 * @property totalWidth The total width of the terminal output in characters
 */
class TerminalUI(private val totalWidth: Int) {

  private val lines = mutableListOf<String>()
  private val ansiRegex = Regex("\u001B\\[[0-9;]*m")

  /**
   * Clears all accumulated lines.
   */
  fun clear() {
    lines.clear()
  }

  /**
   * Adds a line of text to the output.
   *
   * @param text The text to add (empty strings are ignored)
   */
  fun addLine(text: String) {
    if (text.isNotEmpty()) {
      lines.add(text)
    }
  }

  /**
   * Draws a horizontal line with specified border characters.
   *
   * @param left Left border character
   * @param middle Optional middle character (if null, line spans full width)
   * @param right Right border character
   * @param fill Character to fill the line with
   */
  fun drawLine(left: Char, middle: Char?, right: Char, fill: Char) {
    val line = buildString {
      append(left)
      if (middle == null) {
        append(fill.toString().repeat(totalWidth - 2))
      } else {
        append(fill.toString().repeat(totalWidth - 3))
        append(middle)
      }
      append(right)
    }
    lines.add(line)
  }

  /**
   * Draws a full line of repeated characters.
   *
   * @param char Character to repeat across the full width
   */
  fun drawFullLine(char: Char) {
    lines.add(char.toString().repeat(totalWidth))
  }

  /**
   * Adds a centered line of text with borders.
   *
   * @param text Text to center (may contain ANSI color codes)
   * @param leftBorder Left border character
   * @param rightBorder Right border character
   */
  fun addCenteredLine(text: String, leftBorder: Char = '║', rightBorder: Char = '║') {
    val visibleLength = text.visibleLength()
    val padded = text.center(totalWidth - 2, visibleLength)
    lines.add("$leftBorder$padded$rightBorder")
  }

  fun addSplitLine(
      leftText: String,
      rightText: String,
      leftWidth: Int,
      rightWidth: Int,
      leftBorder: Char = '║',
      middleBorder: Char = '│',
      rightBorder: Char = '║'
  ) {
    val leftPadded = truncateOrPadAnsiAware(leftText, leftWidth)
    val rightPadded = truncateOrPadAnsiAware(rightText, rightWidth)
    lines.add("$leftBorder$leftPadded$middleBorder$rightPadded$rightBorder")
  }

  fun addEmptySplitLine(
      leftWidth: Int,
      rightWidth: Int,
      leftBorder: Char = '║',
      middleBorder: Char = '│',
      rightBorder: Char = '║'
  ) {
    val leftSpace = " ".repeat(leftWidth)
    val rightSpace = " ".repeat(rightWidth)
    lines.add("$leftBorder$leftSpace$middleBorder$rightSpace$rightBorder")
  }

  fun drawTable(
      headers: List<String>,
      rows: List<List<String>>,
      colWidths: List<Int>,
      borders: TableBorders = TableBorders()
  ) {
    lines.add(buildTableLine(colWidths, borders.headerTop))
    lines.add(buildTableRow(headers, colWidths, borders.headerRow))
    lines.add(buildTableLine(colWidths, borders.headerBottom))
    rows.forEach { row -> lines.add(buildTableRow(row, colWidths, borders.row)) }
  }

  private fun buildTableLine(colWidths: List<Int>, borderChars: TableBorders.BorderChars): String {
    val sb = StringBuilder()
    sb.append(borderChars.left)
    for ((i, width) in colWidths.withIndex()) {
      sb.append(borderChars.fill.toString().repeat(width))
      sb.append(if (i == colWidths.lastIndex) borderChars.right else borderChars.middle)
    }
    return sb.toString()
  }

  private fun buildTableRow(
      cells: List<String>,
      colWidths: List<Int>,
      borderChars: TableBorders.BorderChars
  ): String {
    val sb = StringBuilder()
    sb.append(borderChars.left)
    for ((i, width) in colWidths.withIndex()) {
      val cellText = truncateOrPadAnsiAware(cells.getOrNull(i) ?: "", width)
      sb.append(cellText)
      sb.append(if (i == colWidths.lastIndex) borderChars.right else borderChars.middle)
    }
    return sb.toString()
  }

  private fun truncateOrPadAnsiAware(text: String, width: Int): String {
    val visible = text.visibleLength()
    return if (visible > width) {
      val clean = text.removeAnsi()
      if (width <= 1) "…" else clean.take(width - 1) + "…"
    } else {
      val pad = " ".repeat(width - visible)
      text + pad
    }
  }

  private fun String.removeAnsi(): String = ansiRegex.replace(this, "")

  private fun String.visibleLength(): Int = this.removeAnsi().length

  private fun String.center(width: Int, visibleLen: Int): String {
    if (visibleLen >= width) return this
    val padding = width - visibleLen
    val padStart = padding / 2
    val padEnd = padding - padStart
    return " ".repeat(padStart) + this + " ".repeat(padEnd)
  }

  fun render(): String = lines.joinToString("\n")

  data class TableBorders(
      val headerTop: BorderChars = BorderChars('┌', '┬', '┐', '─'),
      val headerRow: BorderChars = BorderChars('│', '│', '│', ' '),
      val headerBottom: BorderChars = BorderChars('├', '┼', '┤', '─'),
      val row: BorderChars = BorderChars('│', '│', '│', ' ')
  ) {
    data class BorderChars(val left: Char, val middle: Char, val right: Char, val fill: Char)
  }
}
