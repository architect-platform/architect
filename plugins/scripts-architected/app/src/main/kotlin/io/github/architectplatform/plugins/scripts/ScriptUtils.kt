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
     * Placeholder for command validation.
     *
     * This function is intentionally permissive and returns true for all commands.
     * Commands in the scripts plugin come from trusted configuration files (architect.yml),
     * not from runtime user input, so validation is not strictly necessary.
     *
     * The function is kept as a placeholder for potential future enhancements where
     * administrators might want to enforce additional restrictions on configured commands.
     *
     * @param command The command to validate (currently unused)
     * @return Always returns true
     */
    @Suppress("UNUSED_PARAMETER")
    fun validateCommand(command: String): Boolean {
        // Intentionally permissive - commands come from trusted configuration
        // This is a placeholder for potential future validation logic
        return true
    }
}
