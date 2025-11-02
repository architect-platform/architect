package io.github.architectplatform.plugins.git

/**
 * Utility object for Git-related helper functions.
 */
object GitUtils {
  /**
   * Escapes a shell argument to prevent command injection.
   *
   * @param arg The argument to escape
   * @return The escaped argument safe for shell execution
   */
  fun escapeShellArg(arg: String): String {
    // If the argument contains special characters, wrap it in single quotes
    // and escape any single quotes within it
    return if (arg.matches(Regex("^[a-zA-Z0-9._/:-]+$"))) {
      // Safe characters, no escaping needed
      arg
    } else {
      // Escape single quotes by replacing ' with '\''
      // and wrap the whole thing in single quotes
      "'" + arg.replace("'", "'\\''") + "'"
    }
  }

  /**
   * Validates that a Git config key follows expected format.
   *
   * @param key The config key to validate
   * @return true if the key is valid, false otherwise
   */
  fun isValidGitConfigKey(key: String): Boolean {
    // Git config keys should match pattern: section.subsection.key or section.key
    return key.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*([.-][a-zA-Z][a-zA-Z0-9]*)*$"))
  }
}
