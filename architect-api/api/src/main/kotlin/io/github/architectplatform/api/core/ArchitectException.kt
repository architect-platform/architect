package io.github.architectplatform.api.core

/**
 * Base class for all domain exceptions thrown by the Architect platform.
 *
 * Prefer throwing a specific subclass rather than this directly so that callers
 * can catch and handle error categories precisely.
 *
 * @param message Description of what went wrong
 * @param cause Underlying exception, if any
 */
open class ArchitectException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Thrown when a requested task is not found in the task registry.
 *
 * @param taskId The ID of the task that was not found
 * @param projectName Optional name of the project that was searched
 */
class TaskNotFoundException(
  val taskId: String,
  val projectName: String? = null,
) : ArchitectException(
    buildString {
      append("Task '$taskId' not found")
      if (projectName != null) append(" in project '$projectName'")
    },
  )

/**
 * Thrown when a requested project is not registered with the engine.
 *
 * @param projectName The name of the project that was not found
 */
class ProjectNotFoundException(
  val projectName: String,
) : ArchitectException("Project '$projectName' is not registered")

/**
 * Thrown when task execution fails due to a runtime error.
 *
 * @param taskId The ID of the task that failed
 * @param reason Human-readable reason for the failure
 * @param cause Underlying cause, if any
 */
class TaskExecutionException(
  val taskId: String,
  reason: String,
  cause: Throwable? = null,
) : ArchitectException("Task '$taskId' execution failed: $reason", cause)

/**
 * Thrown when a plugin fails to load or initialize.
 *
 * @param pluginId The ID of the plugin that failed
 * @param reason Human-readable reason for the failure
 * @param cause Underlying cause, if any
 */
class PluginLoadException(
  val pluginId: String,
  reason: String,
  cause: Throwable? = null,
) : ArchitectException("Plugin '$pluginId' failed to load: $reason", cause)

/**
 * Thrown when `architect.yml` configuration is invalid or cannot be parsed.
 *
 * @param reason Description of what is wrong in the configuration
 * @param cause Underlying parse exception, if any
 */
class ConfigurationException(
  reason: String,
  cause: Throwable? = null,
) : ArchitectException("Configuration error: $reason", cause)

/**
 * Thrown when a task's runtime preconditions ([io.github.architectplatform.api.core.tasks.TaskRequirements])
 * are not satisfied.
 *
 * @param taskId The ID of the task whose conditions were not met
 * @param issues Human-readable list of unsatisfied conditions
 */
class TaskConditionException(
  val taskId: String,
  val issues: List<String>,
) : ArchitectException(
    buildString {
      appendLine("Task '$taskId' preconditions not satisfied:")
      issues.forEach { appendLine("  • $it") }
    }.trimEnd(),
  )
