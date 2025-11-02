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
     * Security measures:
     * - Environment variable keys are validated to prevent injection
     * - Environment variable values are properly escaped
     * - Command arguments are properly escaped
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

        // Escape all arguments to prevent command injection
        val escapedArgs = args.map { ScriptUtils.escapeShellArg(it) }
        val argsString = if (escapedArgs.isNotEmpty()) " ${escapedArgs.joinToString(" ")}" else ""
        
        // Build environment variable prefix with proper escaping
        val envPrefix = if (config.environment.isNotEmpty()) {
            try {
                config.environment.entries.joinToString(" ") { (key, value) ->
                    // Validate key format
                    val validatedKey = ScriptUtils.validateEnvKey(key)
                    // Escape value
                    val escapedValue = ScriptUtils.escapeEnvValue(value)
                    "$validatedKey=$escapedValue"
                } + " "
            } catch (e: IllegalArgumentException) {
                return TaskResult.failure(
                    "Script task: $id failed with invalid environment variable: ${e.message}"
                )
            }
        } else {
            ""
        }
        
        // Note: The base command from config is trusted as it comes from configuration,
        // not user input. Arguments and environment variables are always escaped.
        val fullCommand = buildString {
            append(envPrefix)
            append(config.command)
            append(argsString)
        }

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
