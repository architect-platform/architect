package io.github.architectplatform.plugins.scripts

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import kotlin.io.path.Path

/**
 * Task implementation for executing custom shell scripts.
 *
 * Executes a configured shell command with support for arguments, environment variables,
 * and custom working directories.
 *
 * @property scriptName The name of the script (used in task ID)
 * @property config The script configuration containing command, environment, etc.
 * @property phase The workflow phase this task belongs to (optional)
 * @property context The scripts context for global settings
 */
class ScriptTask(
    private val scriptName: String,
    private val config: ScriptConfig,
    private val phase: Phase?,
    private val context: ScriptsContext
) : Task {
    override val id: String = "scripts-$scriptName"

    override fun phase(): Phase? = phase

    override fun description(): String = config.description

    /**
     * Executes the script command with the configured environment and working directory.
     *
     * @param environment Execution environment providing CommandExecutor service
     * @param projectContext The project context
     * @param args Additional arguments to pass to the script command
     * @return TaskResult indicating success or failure
     */
    override fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
        if (!context.enabled) {
            return TaskResult.success("Scripts execution disabled. Skipping script: $scriptName")
        }

        val commandExecutor = environment.service(CommandExecutor::class.java)
        val workingDir =
            Path(projectContext.dir.toString(), config.workingDirectory).toAbsolutePath()

        // Build the full command with arguments
        val argsString = if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""
        
        // Prepend environment variables to command if specified
        val envPrefix = if (config.environment.isNotEmpty()) {
            config.environment.entries.joinToString(" ") { (key, value) ->
                "$key=\"$value\""
            } + " "
        } else {
            ""
        }
        
        val fullCommand = "$envPrefix${config.command}$argsString"

        return try {
            commandExecutor.execute(
                fullCommand,
                workingDir = workingDir.toString()
            )
            TaskResult.success("Script task: $id completed successfully")
        } catch (e: Exception) {
            TaskResult.failure(
                "Script task: $id failed with exception: ${e.message ?: "Unknown error"}"
            )
        }
    }
}
