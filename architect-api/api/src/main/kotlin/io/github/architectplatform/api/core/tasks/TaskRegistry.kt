package io.github.architectplatform.api.core.tasks

/**
 * Registry for managing task registrations and lookups.
 *
 * The TaskRegistry is a central repository where plugins register their tasks during initialization.
 * It provides methods to add tasks, retrieve tasks by ID, and list all registered tasks.
 *
 * Example usage:
 * ```kotlin
 * val registry: TaskRegistry = ...
 * 
 * // Register a task
 * registry.add(MyTask())
 * 
 * // Retrieve a task by ID
 * val task = registry.get("my-task")
 * 
 * // Get all tasks
 * val allTasks = registry.all()
 * ```
 */
interface TaskRegistry {
  /**
   * Registers a task in the registry.
   *
   * @param task The task to register
   * @throws IllegalArgumentException if a task with the same ID is already registered
   */
  fun add(task: Task)

  /**
   * Retrieves a task by its ID.
   *
   * @param id The unique identifier of the task
   * @return The task with the specified ID, or null if no such task exists
   */
  fun get(id: String): Task?

  /**
   * Returns all registered tasks.
   *
   * @return A list of all tasks in the registry
   */
  fun all(): List<Task>
}
