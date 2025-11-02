package io.github.architectplatform.plugins.scripts

/**
 * Context configuration for custom scripts.
 *
 * Defines custom scripts that can be executed as tasks, including their commands,
 * working directories, environment variables, and workflow phase attachments.
 *
 * Example configuration:
 * ```yaml
 * scripts:
 *   scripts:
 *     hello:
 *       command: "echo 'Hello, World!'"
 *       description: "Prints a hello message"
 *       phase: "BUILD"
 *     deploy:
 *       command: "./deploy.sh"
 *       workingDirectory: "scripts"
 *       environment:
 *         ENV: "production"
 *       phase: "PUBLISH"
 *     standalone:
 *       command: "npm run custom-script"
 *       description: "A standalone script without phase"
 * ```
 *
 * @property enabled Whether script execution is enabled globally. Defaults to true.
 * @property scripts Map of script name to script configuration
 */
data class ScriptsContext(
    val enabled: Boolean = true,
    val scripts: Map<String, ScriptConfig> = emptyMap()
)

/**
 * Configuration for an individual script.
 *
 * @property command The shell command to execute
 * @property description Human-readable description of what the script does
 * @property phase Optional workflow phase to attach this script to (e.g., "INIT", "BUILD", "TEST")
 * @property workingDirectory Optional working directory for script execution (relative to project root)
 * @property environment Optional environment variables to set for script execution
 */
data class ScriptConfig(
    val command: String,
    val description: String = "Custom script",
    val phase: String? = null,
    val workingDirectory: String = ".",
    val environment: Map<String, String> = emptyMap()
)
