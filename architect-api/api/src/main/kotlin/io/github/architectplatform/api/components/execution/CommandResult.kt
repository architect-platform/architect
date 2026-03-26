package io.github.architectplatform.api.components.execution

/**
 * Represents the result of a shell command execution.
 *
 * Captures the exit code, standard output, standard error, and execution duration
 * of a command, providing structured access to command execution outcomes.
 *
 * Example usage:
 * ```kotlin
 * val executor = environment.service(CommandExecutor::class.java)
 * val result = executor.executeWithResult("npm test", "/path/to/project")
 *
 * if (result.exitCode == 0) {
 *   println("Tests passed: ${result.stdout}")
 * } else {
 *   println("Tests failed: ${result.stderr}")
 * }
 * println("Completed in ${result.durationMs}ms")
 * ```
 *
 * @property exitCode The process exit code (0 typically indicates success)
 * @property stdout The standard output of the command
 * @property stderr The standard error output of the command
 * @property durationMs The wall-clock duration of the command execution in milliseconds
 */
data class CommandResult(
  val exitCode: Int,
  val stdout: String,
  val stderr: String = "",
  val durationMs: Long = 0,
) {
  /**
   * Whether the command completed successfully (exit code 0).
   */
  val success: Boolean
    get() = exitCode == 0
}
