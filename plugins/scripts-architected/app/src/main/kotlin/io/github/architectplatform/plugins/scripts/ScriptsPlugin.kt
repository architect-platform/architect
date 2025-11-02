package io.github.architectplatform.plugins.scripts

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Architect plugin for custom script execution.
 *
 * Provides the ability to define and execute custom shell scripts through the Architect
 * workflow system, with support for:
 * - Custom script definitions with configurable commands
 * - Attachment to workflow phases (INIT, BUILD, TEST, RUN, RELEASE, PUBLISH)
 * - Standalone script execution without phase association
 * - Environment variable configuration
 * - Custom working directory support
 * - Command-line argument passing
 *
 * Example configuration:
 * ```yaml
 * scripts:
 *   enabled: true
 *   scripts:
 *     hello:
 *       command: "echo 'Hello, World!'"
 *       description: "Prints a greeting"
 *       phase: "BUILD"
 *     deploy:
 *       command: "./deploy.sh"
 *       description: "Deploys the application"
 *       workingDirectory: "scripts"
 *       environment:
 *         ENV: "production"
 *         REGION: "us-east-1"
 *       phase: "PUBLISH"
 *     custom:
 *       command: "npm run custom-task"
 *       description: "Runs a custom npm script"
 * ```
 *
 * Scripts can be executed via CLI:
 * ```bash
 * architect scripts-hello
 * architect scripts-deploy -- arg1 arg2
 * architect scripts-custom
 * ```
 */
class ScriptsPlugin : ArchitectPlugin<ScriptsContext> {
    override val id = "scripts-plugin"
    override val contextKey: String = "scripts"
    override val ctxClass: Class<ScriptsContext> = ScriptsContext::class.java
    override var context: ScriptsContext = ScriptsContext()

    /**
     * Registers script tasks with the task registry.
     *
     * This method dynamically creates and registers tasks based on the scripts
     * defined in the context configuration. Each script becomes a task with ID
     * "scripts-{scriptName}".
     *
     * Scripts can optionally be attached to workflow phases by setting the "phase"
     * property in their configuration. Supported phases include:
     * - INIT: Initialization tasks
     * - LINT: Code quality and linting
     * - VERIFY: Verification and validation
     * - BUILD: Building and compilation
     * - TEST: Testing
     * - RUN: Running the application
     * - RELEASE: Release preparation
     * - PUBLISH: Publishing and deployment
     *
     * @param registry The task registry to add script tasks to
     */
    override fun register(registry: TaskRegistry) {
        for ((scriptName, scriptConfig) in context.scripts) {
            // Parse phase from configuration if specified
            val phase = scriptConfig.phase?.let { parsePhase(it) }
            
            // Register the script task
            registry.add(
                ScriptTask(
                    scriptName = scriptName,
                    config = scriptConfig,
                    phase = phase,
                    context = context
                )
            )
        }
    }

    /**
     * Parses a phase name string into a Phase object.
     *
     * Supports phase names in various formats:
     * - Uppercase: "BUILD", "TEST", etc.
     * - Lowercase: "build", "test", etc.
     * - Mixed case: "Build", "Test", etc.
     *
     * @param phaseName The phase name to parse
     * @return The corresponding Phase object, or null if the phase name is not recognized
     */
    private fun parsePhase(phaseName: String): Phase? {
        return try {
            CodeWorkflow.valueOf(phaseName.uppercase())
        } catch (e: IllegalArgumentException) {
            // Log warning but don't fail - allow standalone scripts
            println("Warning: Unknown phase '$phaseName' for script. Script will be standalone.")
            null
        }
    }
}
