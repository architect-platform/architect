package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.core.logging.ArchitectLogger

/**
 * Execution environment providing access to services and event publishing.
 *
 * The Environment interface abstracts the execution context, allowing tasks to access
 * platform services through dependency injection and to communicate via events.
 *
 * Example usage:
 * ```kotlin
 * fun execute(environment: Environment, projectContext: ProjectContext): TaskResult {
 *   // Access a service
 *   val executor = environment.service(CommandExecutor::class.java)
 *   executor.execute("npm install")
 *
 *   // Structured logging
 *   val logger = environment.logger("my-task")
 *   logger.info("Build completed")
 *
 *   // Publish an event
 *   environment.publish(BuildCompletedEvent())
 *
 *   return TaskResult.success()
 * }
 * ```
 */
interface Environment {
  /**
   * Retrieves a service instance by type.
   *
   * Services are typically implementations of component interfaces (like CommandExecutor,
   * ResourceExtractor) that are provided by the platform.
   *
   * @param type The class type of the service to retrieve
   * @return An instance of the requested service
   * @throws IllegalArgumentException if no service of the specified type is available
   */
  fun <T> service(type: Class<T>): T

  /**
   * Publishes an event to the event system.
   *
   * Events allow tasks to communicate and notify other components about state changes
   * or significant occurrences during execution.
   *
   * @param event The event object to publish
   */
  fun publish(event: Any)

  /**
   * Resolves a named secret from the active runtime environment.
   *
   * Implementations may source secrets from environment variables, local `.env` files,
   * or external secret managers.
   *
   * @param name The secret identifier
   * @return The resolved secret value, or null when it is unavailable
   */
  fun secret(name: String): String? = null

  /**
   * Returns the active environment profile name.
   *
   * Profiles allow different configurations for dev, staging, production, CI, etc.
   * Returns "default" when no profile is explicitly set.
   *
   * @return The active profile name (e.g., "staging", "ci", "default")
   */
  fun profile(): String = "default"

  /**
   * Creates an [ArchitectLogger] for structured logging with the given tag.
   *
   * Tasks should use this instead of directly using SLF4J or println.
   * The engine routes log output through the execution event system.
   *
   * @param tag A short identifier for the logger (typically the task ID)
   * @return An ArchitectLogger instance for the given tag
   */
  fun logger(tag: String): ArchitectLogger = object : ArchitectLogger {
    override val tag: String = tag
    override fun debug(message: String) {}
    override fun info(message: String) {}
    override fun warn(message: String) {}
    override fun error(message: String) {}
    override fun error(
      message: String,
      throwable: Throwable,
    ) {}
  }

  /**
   * Subscribes to events of a specific type.
   *
   * Enables inter-task communication without tight coupling. Tasks can subscribe
   * to event types and react when those events are published by other tasks or
   * the engine.
   *
   * Example usage:
   * ```kotlin
   * environment.subscribe(BuildCompletedEvent::class.java) { event ->
   *   println("Build completed: ${event.artifactPath}")
   * }
   * ```
   *
   * @param E The event type to subscribe to
   * @param type The class of the event type
   * @param handler The handler function invoked when an event of this type is published
   */
  fun <E> subscribe(
    type: Class<E>,
    handler: (E) -> Unit,
  ) {
    // No-op default. Engine implementation routes events to handlers.
  }
}
