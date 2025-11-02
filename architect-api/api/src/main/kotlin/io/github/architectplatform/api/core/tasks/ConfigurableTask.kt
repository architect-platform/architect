package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * A task that supports configuration through a key-value map.
 *
 * ConfigurableTask enables tasks to be parameterized with configuration values,
 * making them more flexible and reusable. Configuration can be:
 * - Read from project configuration files
 * - Passed programmatically during task creation
 * - Used to customize task behavior without code changes
 *
 * This is particularly useful for:
 * - Plugin tasks that need user-specific settings
 * - Tasks that should behave differently in different environments
 * - Reusable tasks with customizable behavior
 * - Tasks with sensible defaults that can be overridden
 *
 * Example usage:
 * ```kotlin
 * val deployTask = ConfigurableTask(
 *   id = "deploy",
 *   description = "Deploy application",
 *   phase = CoreWorkflow.PUBLISH,
 *   config = mapOf(
 *     "environment" to "production",
 *     "timeout" to "300",
 *     "retries" to "3"
 *   )
 * ) { env, ctx, cfg, args ->
 *   val environment = cfg["environment"] ?: "development"
 *   val timeout = cfg["timeout"]?.toIntOrNull() ?: 60
 *   // Use configuration...
 *   TaskResult.success("Deployed to $environment")
 * }
 * ```
 *
 * @property id Unique identifier for this task
 * @property description Human-readable description
 * @property phase The lifecycle phase this task belongs to (optional)
 * @property config Configuration map with task-specific settings
 * @property customDependencies Additional dependencies beyond phase dependencies (optional)
 * @property task Lambda function containing the task logic with access to configuration
 */
class ConfigurableTask(
  override val id: String,
  private val description: String,
  private val phase: Phase? = null,
  private val config: Map<String, String> = emptyMap(),
  private val customDependencies: List<String> = emptyList(),
  private val task: (Environment, ProjectContext, Map<String, String>, List<String>) -> TaskResult,
) : Task {
  override fun description(): String = description

  override fun phase(): Phase? = phase

  override fun depends(): List<String> {
    // Combine phase dependencies with custom dependencies
    val phaseDeps = phase?.depends() ?: emptyList()
    return (phaseDeps + customDependencies).distinct()
  }

  /**
   * Returns the configuration map for this task.
   *
   * @return Map of configuration key-value pairs
   */
  fun config(): Map<String, String> = config

  /**
   * Gets a configuration value by key with an optional default.
   *
   * @param key The configuration key
   * @param default The default value if the key is not found
   * @return The configuration value or default
   */
  fun getConfig(key: String, default: String? = null): String? = config[key] ?: default

  /**
   * Gets a required configuration value by key.
   *
   * @param key The configuration key
   * @return The configuration value
   * @throws IllegalArgumentException if the key is not found
   */
  fun getRequiredConfig(key: String): String =
    config[key] ?: throw IllegalArgumentException("Required configuration '$key' not found in task '$id'")

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    return task(environment, projectContext, config, args)
  }
}
