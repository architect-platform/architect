package io.github.architectplatform.api.core.utils

/**
 * Utilities for safely constructing shell command arguments.
 *
 * All user-provided or config-derived values passed to shell commands MUST be escaped
 * through [escapeShellArg] before interpolation into a command string to prevent
 * shell injection attacks.
 *
 * Usage:
 * ```kotlin
 * val cmd = "my-tool ${ShellUtils.escapeShellArg(userInput)}"
 * val cmd = "my-tool ${ShellUtils.escapeShellArgs(userArgs)}"
 * ```
 */
object ShellUtils {

  /**
   * Escapes a single shell argument by wrapping it in single quotes.
   *
   * This is safe for any argument value including spaces, dollar signs, backticks,
   * semicolons, and other shell metacharacters. Any embedded single-quote character
   * is escaped using the `'\''` technique.
   *
   * Examples:
   * - `"hello"` → `'hello'`
   * - `"hello world"` → `'hello world'`
   * - `"it's"` → `'it'\''s'`
   * - `"; rm -rf /"` → `'; rm -rf /'`
   * - `"$(evil)"` → `'$(evil)'`
   */
  fun escapeShellArg(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"

  /**
   * Escapes a list of shell arguments and joins them with spaces.
   *
   * Equivalent to calling [escapeShellArg] on each element and joining with a space.
   */
  fun escapeShellArgs(args: List<String>): String = args.joinToString(" ") { escapeShellArg(it) }

  /**
   * Validates that a string value consists only of safe alphanumeric characters, hyphens,
   * underscores, and dots — suitable for values used as flags or identifiers (e.g.,
   * Rust profile names, package manager names, Nx target names).
   *
   * Returns the original value if safe, or throws [IllegalArgumentException] otherwise.
   */
  fun requireSafeIdentifier(value: String, description: String): String {
    require(value.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
      "Unsafe $description: '$value'. Only alphanumeric characters, hyphens, underscores, and dots are allowed."
    }
    return value
  }
}
