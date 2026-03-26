package io.github.architectplatform.api.components.execution

/**
 * Interface for executing shell commands.
 *
 * CommandExecutor provides an abstraction for running system commands, allowing tasks
 * to execute shell commands without directly depending on process execution APIs.
 *
 * Two execution modes are available:
 * - [execute]: Fire-and-forget style; throws on non-zero exit code (legacy behavior)
 * - [executeWithResult]: Returns a structured [CommandResult] with exit code, stdout, stderr, and duration
 *
 * Example usage:
 * ```kotlin
 * val executor = environment.service(CommandExecutor::class.java)
 *
 * // Legacy: execute and throw on failure
 * executor.execute("npm install", "/path/to/project")
 *
 * // Structured: get full result
 * val result = executor.executeWithResult("npm test", "/path/to/project")
 * if (result.success) println("stdout: ${result.stdout}")
 * ```
 */
interface CommandExecutor {
  /**
   * Executes a shell command, throwing an exception on non-zero exit code.
   *
   * @param command The command string to execute
   * @param workingDir Optional working directory path for command execution.
   *                   If null, uses the current working directory.
   * @throws IllegalStateException if the command returns a non-zero exit code
   */
  fun execute(
    command: String,
    workingDir: String? = null,
  )

  /**
   * Executes a shell command and returns a structured [CommandResult].
   *
   * Unlike [execute], this method does not throw on non-zero exit codes.
   * The caller is responsible for checking [CommandResult.success].
   *
   * @param command The command string to execute
   * @param workingDir Optional working directory path. Defaults to current directory.
   * @param timeoutSeconds Maximum execution time in seconds. Defaults to 300 (5 minutes).
   * @param env Additional environment variables to set for the command. Defaults to inheriting the current environment.
   * @return A [CommandResult] with exit code, stdout, stderr, and duration
   */
  fun executeWithResult(
    command: String,
    workingDir: String? = null,
    timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    env: Map<String, String> = emptyMap(),
  ): CommandResult {
    // Default implementation delegates to execute() for backward compatibility.
    // Implementations should override this for full functionality.
    val startTime = System.currentTimeMillis()
    return try {
      execute(command, workingDir)
      CommandResult(
        exitCode = 0,
        stdout = "",
        durationMs = System.currentTimeMillis() - startTime,
      )
    } catch (e: Exception) {
      CommandResult(
        exitCode = 1,
        stdout = "",
        stderr = e.message ?: "Command execution failed",
        durationMs = System.currentTimeMillis() - startTime,
      )
    }
  }

  companion object {
    /** Default command timeout in seconds (5 minutes). */
    const val DEFAULT_TIMEOUT_SECONDS: Long = 300
  }
}
