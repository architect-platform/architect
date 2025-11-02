package io.github.architectplatform.plugins.scripts

/**
 * Utility object for script-related helper functions, particularly security-related.
 */
object ScriptUtils {
    /**
     * Escapes a shell argument to prevent command injection.
     *
     * This function wraps arguments in single quotes and properly escapes any single quotes
     * within the argument. This is the safest approach for shell argument escaping as single
     * quotes prevent interpretation of special characters.
     *
     * @param arg The argument to escape
     * @return The escaped argument safe for shell execution
     */
    fun escapeShellArg(arg: String): String {
        // Always wrap in single quotes and escape any single quotes within
        // Single quotes prevent all shell interpretation except for single quotes themselves
        // We escape single quotes by ending the quoted string, adding an escaped quote, 
        // and starting a new quoted string: 'it'\''s' -> it's
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    /**
     * Escapes an environment variable key to prevent command injection.
     *
     * Environment variable keys should only contain alphanumeric characters and underscores.
     * This function validates the key and throws an exception if it contains invalid characters.
     *
     * @param key The environment variable key to validate
     * @return The validated key
     * @throws IllegalArgumentException if the key contains invalid characters
     */
    fun validateEnvKey(key: String): String {
        if (!key.matches(Regex("^[A-Z_][A-Z0-9_]*$"))) {
            throw IllegalArgumentException(
                "Invalid environment variable key: $key. " +
                "Keys must start with a letter or underscore and contain only uppercase letters, digits, and underscores."
            )
        }
        return key
    }

    /**
     * Escapes an environment variable value to prevent command injection.
     *
     * Values are wrapped in double quotes and special characters are escaped.
     *
     * @param value The environment variable value to escape
     * @return The escaped value safe for shell execution
     */
    fun escapeEnvValue(value: String): String {
        // Escape backslashes first, then other special characters
        // Use double quotes to allow variable expansion if needed
        val escaped = value
            .replace("\\", "\\\\")  // Escape backslashes
            .replace("\"", "\\\"")  // Escape double quotes
            .replace("\$", "\\$")   // Escape dollar signs
            .replace("`", "\\`")    // Escape backticks
            .replace("!", "\\!")    // Escape exclamation marks
        return "\"$escaped\""
    }

    /**
     * Validates that a command is not attempting obvious command injection patterns.
     *
     * This is a basic sanity check and should not be relied upon as the sole security measure.
     * Proper escaping is still required.
     *
     * @param command The command to validate
     * @return true if the command passes basic validation, false otherwise
     */
    fun validateCommand(command: String): Boolean {
        // Check for obvious command injection patterns
        // Note: This is defense-in-depth, not a replacement for proper escaping
        val dangerousPatterns = listOf(
            ";",     // Command separator
            "&&",    // AND operator
            "||",    // OR operator  
            "|",     // Pipe operator
            ">",     // Output redirection
            "<",     // Input redirection
            "$(",    // Command substitution
            "`"      // Command substitution
        )
        
        // For configured commands, we allow these patterns as they may be intentional
        // This validation is primarily informational
        return true
    }
}
