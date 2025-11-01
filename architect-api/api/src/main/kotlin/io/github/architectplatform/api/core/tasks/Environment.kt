package io.github.architectplatform.api.core.tasks

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
}
