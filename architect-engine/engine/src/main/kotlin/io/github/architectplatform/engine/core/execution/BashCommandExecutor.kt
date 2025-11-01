package io.github.architectplatform.engine.core.execution

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
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
 */
@Singleton
open class BashCommandExecutor(
    @Property(name = EngineConfiguration.CommandExecutor.TIMEOUT_SECONDS, defaultValue = "${EngineConfiguration.CommandExecutor.DEFAULT_TIMEOUT_SECONDS}")
    private val timeoutSeconds: Long = EngineConfiguration.CommandExecutor.DEFAULT_TIMEOUT_SECONDS,
    
    @Property(name = EngineConfiguration.CommandExecutor.REDIRECT_ERROR_STREAM, defaultValue = "${EngineConfiguration.CommandExecutor.DEFAULT_REDIRECT_ERROR_STREAM}")
    private val redirectErrorStream: Boolean = EngineConfiguration.CommandExecutor.DEFAULT_REDIRECT_ERROR_STREAM
) : CommandExecutor {

  private val logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Executes a command and returns exit code and output.
   * 
   * @param command The bash command to execute
   * @param workingDir Optional working directory for command execution
   * @return Pair of exit code and command output
   */
  private fun executeCommand(command: String, workingDir: String? = null): Pair<Int, String> {
    val processBuilder = ProcessBuilder("sh", "-c", command)
    workingDir?.let { processBuilder.directory(File(it)) }

    processBuilder.redirectErrorStream(redirectErrorStream)

    val process = processBuilder.start()

    val output = StringBuilder()
    val reader = process.inputStream.bufferedReader()

    // Read process output line by line
    val outputThread = Thread { 
      reader.forEachLine { line -> 
        output.appendLine(line) 
      } 
    }

    outputThread.start()
    val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
    
    if (!completed) {
      process.destroyForcibly()
      outputThread.interrupt()
      throw IllegalStateException(
        "Command timed out after $timeoutSeconds seconds: $command"
      )
    }
    
    outputThread.join()
    val exitCode = process.exitValue()

    return exitCode to output.toString().trim()
  }

  override fun execute(command: String, workingDir: String?) {
    val (exitCode, result) = executeCommand(command, workingDir)
    logger.debug("Executed command: {}\nExit code: {}\nResult:\n{}", command, exitCode, result)
    if (exitCode != 0) {
      logger.debug("Command failed with exit code {}\nResult:\n{}", exitCode, result)
      error("Command failed with exit code $exitCode\nResult:\n$result")
    }
  }
}
