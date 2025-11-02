package io.github.architectplatform.plugins.javascriptarchitected

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import kotlin.io.path.Path

/**
 * Task implementation for executing JavaScript/Node.js commands via package managers.
 *
 * Executes npm/yarn/pnpm commands in the configured working directory.
 *
 * @property command The command to execute (e.g., "install", "build", "test")
 * @property phase The workflow phase this task belongs to
 * @property context The JavaScript context containing package manager configuration
 */
class JavaScriptTask(
    private val command: String,
    private val phase: Phase,
    private val context: JavaScriptContext
) : Task {
  override val id: String = "javascript-$command"

  override fun phase(): Phase = phase

  /**
   * Executes the JavaScript command using the configured package manager.
   *
   * @param environment Execution environment providing CommandExecutor service
   * @param projectContext The project context
   * @param args Additional arguments to pass to the command
   * @return TaskResult indicating success or failure
   */
  override fun execute(
      environment: Environment,
      projectContext: ProjectContext,
      args: List<String>
  ): TaskResult {
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val workingDir =
        Path(projectContext.dir.toString(), context.workingDirectory).toAbsolutePath()

    val fullCommand = buildCommand(command, args)

    try {
      commandExecutor.execute(fullCommand, workingDir = workingDir.toString())
    } catch (e: Exception) {
      return TaskResult.failure(
          "JavaScript task: $id failed with exception: ${e.message}")
    }

    return TaskResult.success("JavaScript task: $id completed successfully")
  }

  /**
   * Builds the full command string based on package manager and command type.
   *
   * @param command The base command (install, build, test, run, etc.)
   * @param args Additional arguments
   * @return The complete command string to execute
   */
  private fun buildCommand(command: String, args: List<String>): String {
    val packageManager = context.packageManager
    val argsString = if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""

    return when (command) {
      "install" -> "$packageManager install$argsString"
      "ci" -> when (packageManager) {
        "npm" -> "npm ci$argsString"
        "yarn" -> "yarn install --frozen-lockfile$argsString"
        "pnpm" -> "pnpm install --frozen-lockfile$argsString"
        else -> "$packageManager install$argsString"
      }
      "build" -> when (packageManager) {
        "npm" -> "npm run build$argsString"
        "yarn" -> "yarn build$argsString"
        "pnpm" -> "pnpm build$argsString"
        else -> "$packageManager run build$argsString"
      }
      "test" -> when (packageManager) {
        "npm" -> "npm test$argsString"
        "yarn" -> "yarn test$argsString"
        "pnpm" -> "pnpm test$argsString"
        else -> "$packageManager test$argsString"
      }
      "lint" -> when (packageManager) {
        "npm" -> "npm run lint$argsString"
        "yarn" -> "yarn lint$argsString"
        "pnpm" -> "pnpm lint$argsString"
        else -> "$packageManager run lint$argsString"
      }
      "dev" -> when (packageManager) {
        "npm" -> "npm run dev$argsString"
        "yarn" -> "yarn dev$argsString"
        "pnpm" -> "pnpm dev$argsString"
        else -> "$packageManager run dev$argsString"
      }
      "start" -> when (packageManager) {
        "npm" -> "npm start$argsString"
        "yarn" -> "yarn start$argsString"
        "pnpm" -> "pnpm start$argsString"
        else -> "$packageManager start$argsString"
      }
      else -> "$packageManager run $command$argsString"
    }
  }
}
