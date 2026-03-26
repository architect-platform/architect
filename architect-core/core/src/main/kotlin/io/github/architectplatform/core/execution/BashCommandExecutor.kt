package io.github.architectplatform.core.execution

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.core.config.EngineConfiguration
import java.io.File
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * Executes bash commands with configurable timeout and output handling.
 * 
 * This executor provides:
 * - Configurable command timeout
 * - Optional error stream redirection
 * - Detailed logging of command execution
 * - Thread-safe command execution
 * - Structured [CommandResult] via [executeWithResult]
 */
open class BashCommandExecutor(
    private val timeoutSeconds: Long = EngineConfiguration.CommandExecutor.DEFAULT_TIMEOUT_SECONDS,
    private val redirectErrorStream: Boolean = EngineConfiguration.CommandExecutor.DEFAULT_REDIRECT_ERROR_STREAM
) : CommandExecutor {

  private val logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Executes a command and returns exit code, stdout, and stderr.
   */
  private fun executeCommand(
      command: String,
      workingDir: String? = null,
      timeout: Long = timeoutSeconds,
      env: Map<String, String> = emptyMap(),
  ): Triple<Int, String, String> {
    val launchedProcess = SandboxedProcessLauncher.launch(
      command = listOf("sh", "-c", command),
      workingDir = workingDir,
      redirectErrorStream = redirectErrorStream,
      env = env,
    )
    val process = launchedProcess.process

    val stdout = StringBuilder()
    val stderr = StringBuilder()
    val stdoutReader = process.inputStream.bufferedReader()
    val stderrReader = if (!redirectErrorStream) process.errorStream.bufferedReader() else null

    val stdoutThread = Thread { 
      stdoutReader.forEachLine { line -> 
        stdout.appendLine(line) 
      } 
    }
    val stderrThread = stderrReader?.let {
      Thread { it.forEachLine { line -> stderr.appendLine(line) } }
    }

    stdoutThread.start()
    stderrThread?.start()
    val completed = process.waitFor(timeout, TimeUnit.SECONDS)
    
    try {
      if (!completed) {
        process.destroyForcibly()
        stdoutThread.interrupt()
        stderrThread?.interrupt()
        stdoutThread.join(1000)
        stderrThread?.join(1000)
        throw IllegalStateException(
          "Command timed out after $timeout seconds: $command"
        )
      }

      stdoutThread.join()
      stderrThread?.join()
      val exitCode = process.exitValue()

      return Triple(exitCode, stdout.toString().trim(), stderr.toString().trim())
    } finally {
      launchedProcess.cleanup()
    }
  }

  override fun execute(command: String, workingDir: String?) {
    val (exitCode, stdout, stderr) = executeCommand(command, workingDir)
    val output = if (redirectErrorStream) stdout else "$stdout\n$stderr".trim()
    logger.debug("Executed command: {}\nExit code: {}\nResult:\n{}", command, exitCode, output)
    if (exitCode != 0) {
      logger.debug("Command failed with exit code {}\nResult:\n{}", exitCode, output)
      error("Command failed with exit code $exitCode\nResult:\n$output")
    }
  }

  override fun executeWithResult(
      command: String,
      workingDir: String?,
      timeoutSeconds: Long,
      env: Map<String, String>,
  ): CommandResult {
    val startTime = System.currentTimeMillis()
    return try {
      val (exitCode, stdout, stderr) = executeCommand(command, workingDir, timeoutSeconds, env)
      val durationMs = System.currentTimeMillis() - startTime
      logger.debug("Executed command: {}\nExit code: {}\nDuration: {}ms", command, exitCode, durationMs)
      CommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr, durationMs = durationMs)
    } catch (e: IllegalStateException) {
      val durationMs = System.currentTimeMillis() - startTime
      CommandResult(exitCode = -1, stdout = "", stderr = e.message ?: "Command timed out", durationMs = durationMs)
    }
  }
}
