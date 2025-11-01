package io.github.architectplatform.api.components.execution

/**
 * Interface for executing shell commands.
 *
 * CommandExecutor provides an abstraction for running system commands, allowing tasks
 * to execute shell commands without directly depending on process execution APIs.
 *
 * Example usage:
 * ```kotlin
 * val executor = environment.service(CommandExecutor::class.java)
 *
 * // Execute a command in the current directory
 * executor.execute("npm install")
 *
 * // Execute a command in a specific directory
 * executor.execute("gradle build", "/path/to/project")
 * ```
 */
interface CommandExecutor {
  /**
   * Executes a shell command.
   *
   * @param command The command string to execute
   * @param workingDir Optional working directory path for command execution.
   *                   If null, uses the current working directory.
   */
  fun execute(
    command: String,
    workingDir: String? = null,
  )
}
