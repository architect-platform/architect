package io.github.architectplatform.plugins.git

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Task implementation for executing Git commands.
 *
 * Executes a Git command in the project directory, proxying the command
 * execution through the Architect framework.
 *
 * @property command The Git command to execute (e.g., "status", "commit", "push")
 * @property phase The workflow phase this task belongs to
 * @property context The Git context containing configuration
 */
class GitTask(
    private val command: String,
    private val phase: Phase,
    private val context: GitContext
) : Task {
  override val id: String = "git-$command"

  override fun phase(): Phase = phase

  /**
   * Executes the Git command in the project directory.
   *
   * @param environment Execution environment providing CommandExecutor service
   * @param projectContext The project context
   * @param args Additional arguments to pass to the Git command
   * @return TaskResult indicating success or failure
   */
  override fun execute(
      environment: Environment,
      projectContext: ProjectContext,
      args: List<String>
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.success("Git task: $id disabled. Skipping...")
    }

    val commandExecutor = environment.service(CommandExecutor::class.java)

    return try {
      // Escape arguments to prevent command injection
      val escapedArgs = args.map { escapeShellArg(it) }.joinToString(" ")
      val fullCommand = "git $command $escapedArgs"
      commandExecutor.execute(fullCommand, workingDir = projectContext.dir.toString())
      TaskResult.success("Git task: $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure(
          "Git task: $id failed with exception: ${e.message ?: "Unknown error"}")
    }
  }

  /**
   * Escapes a shell argument to prevent command injection.
   * 
   * @param arg The argument to escape
   * @return The escaped argument safe for shell execution
   */
  private fun escapeShellArg(arg: String): String {
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
}
