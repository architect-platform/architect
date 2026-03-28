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
   * Registers an alias that resolves to another task.
   *
   * Alias IDs participate in lookups and task listings, but preserve the original task ID.
   */
  fun addAlias(
    aliasId: String,
    targetId: String,
    description: String = "Alias for $targetId",
  ) {
    throw UnsupportedOperationException("Task aliases are not supported by this registry")
  }

  /**
   * Registers a synthetic group task that depends on multiple member tasks.
   */
  fun addGroup(
    groupId: String,
    memberIds: List<String>,
    description: String = "Group task: $groupId",
  ) {
    throw UnsupportedOperationException("Task groups are not supported by this registry")
  }

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

  /**
   * Resolves a task reference into one or more tasks.
   *
   * Implementations may expand task groups (`group`, `group:*`, `group:member`) and aliases.
   * The default implementation preserves legacy behaviour by falling back to a direct ID lookup.
   */
  fun resolve(reference: String): List<Task> = get(reference)?.let(::listOf) ?: emptyList()

  /**
   * Returns registered task groups keyed by group ID.
   */
  fun groups(): Map<String, List<String>> = emptyMap()
}
