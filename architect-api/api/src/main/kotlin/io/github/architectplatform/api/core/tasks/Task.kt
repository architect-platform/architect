package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Core abstraction for a unit of work in the Architect platform.
 *
 * Tasks are the fundamental building blocks of the Architect execution model. Each task represents
 * a discrete unit of work that can be executed as part of a project's lifecycle. Tasks can be:
 * - **Standalone**: Independent tasks without phase or parent
 * - **Phase tasks**: Tasks organized into lifecycle phases (INIT, BUILD, TEST, etc.)
 * - **Composite tasks**: Tasks that contain and orchestrate child tasks
 * - **Configurable tasks**: Tasks with customizable configuration options
 *
 * Tasks support flexible relationships:
 * - **Dependencies**: Tasks that must complete before this task starts
 * - **Children**: Subtasks that execute as part of this task
 * - **Phase membership**: Automatic dependency inheritance from phases
 *
 * Example usage:
 * ```kotlin
 * // Simple standalone task
 * class MyTask : Task {
 *   override val id = "my-task"
 *
 *   override fun description() = "Performs my custom task"
 *
 *   override fun execute(environment: Environment, projectContext: ProjectContext, args: List<String>): TaskResult {
 *     // Task implementation
 *     return TaskResult.success("Task completed successfully")
 *   }
 * }
 *
 * // Composite task with children
 * val buildTask = CompositeTask(
 *   id = "build-all",
 *   description = "Build all components",
 *   children = listOf("compile", "package", "verify")
 * )
 * ```
 */
interface Task {
  /**
   * Unique identifier for this task.
   *
   * The ID should be unique within the task registry and is used to reference this task
   * in dependency declarations and execution requests.
   */
  val id: String

  /**
   * Returns a human-readable description of what this task does.
   *
   * @return A description of the task's purpose and behavior
   */
  fun description(): String = "No description provided for task $id"

  /**
   * Returns the phase this task belongs to, if any.
   *
   * Tasks associated with a phase inherit the phase's dependencies and are executed
   * in the order defined by the phase hierarchy.
   *
   * @return The phase this task belongs to, or null if the task is not associated with a phase
   */
  fun phase(): Phase? = null

  /**
   * Returns a list of task IDs that this task depends on.
   *
   * Dependencies are resolved transitively, meaning if this task depends on Task A,
   * and Task A depends on Task B, then Task B will be executed before this task.
   *
   * By default, a task inherits the dependencies of its phase.
   *
   * @return List of task IDs that must be executed before this task
   */
  fun depends(): List<String> = phase()?.depends() ?: emptyList()

  /**
   * Returns a list of child task IDs that belong to this task.
   *
   * Child tasks are subtasks that are part of this task's execution. When this task
   * is executed, its children will be executed in order as part of the parent task.
   * This enables hierarchical task organization where a task can be composed of multiple subtasks.
   *
   * Unlike dependencies (which must complete before this task), children are executed
   * as part of this task's execution.
   *
   * @return List of child task IDs, or empty list if this task has no children
   */
  fun children(): List<String> = emptyList()

  /**
   * Executes the task's work.
   *
   * This method contains the main logic of the task. It receives the execution environment,
   * project context, and optional command-line arguments.
   *
   * @param environment The execution environment providing access to services and event publishing
   * @param projectContext Context containing project directory and configuration
   * @param args Optional command-line arguments passed to the task
   * @return A TaskResult indicating success or failure, with an optional message and sub-results
   */
  fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String> = emptyList(),
  ): TaskResult
}
