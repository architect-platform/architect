package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * A task that can contain and execute child tasks.
 *
 * CompositeTask enables hierarchical task organization, where a parent task can orchestrate
 * the execution of multiple child tasks. This is useful for:
 * - Grouping related tasks into a single logical unit
 * - Creating reusable task compositions
 * - Building complex workflows with subtasks
 * - Defining task hierarchies independent of phases
 *
 * Child tasks can be:
 * - Other composite tasks (creating deep hierarchies)
 * - Simple tasks or tasks with arguments
 * - Any implementation of the Task interface
 *
 * **Important:** Child tasks are executed by the TaskExecutor after the parent task's execute()
 * method completes. The hooks (beforeChildren, afterChildren) run as part of the parent task's
 * execute() method, so they don't have direct access to child execution results. To handle
 * child results, check the aggregated TaskResult returned by TaskExecutor.
 *
 * Example usage:
 * ```kotlin
 * val buildTask = CompositeTask(
 *   id = "build-all",
 *   description = "Build all components",
 *   phase = CoreWorkflow.BUILD,
 *   children = listOf("compile", "package", "verify-artifacts")
 * ) { env, ctx, childResults ->
 *   // Optional: custom logic before children execute
 *   TaskResult.success("Prepared for build")
 * }
 * ```
 *
 * @property id Unique identifier for this task
 * @property description Human-readable description of what this task does
 * @property phase The lifecycle phase this task belongs to (optional)
 * @property children List of child task IDs to execute
 * @property customDependencies Additional dependencies beyond phase dependencies (optional)
 * @property beforeChildren Optional hook executed before child tasks (optional)
 * @property afterChildren Optional hook executed after the parent task completes (optional)
 * Note: afterChildren runs before child tasks are executed, as it's part of parent's execute()
 */
class CompositeTask(
  override val id: String,
  private val description: String,
  private val phase: Phase? = null,
  private val children: List<String> = emptyList(),
  private val customDependencies: List<String> = emptyList(),
  private val beforeChildren: ((Environment, ProjectContext) -> TaskResult)? = null,
  private val afterChildren: ((Environment, ProjectContext, List<TaskResult>) -> TaskResult)? = null,
) : Task {
  override fun description(): String = description

  override fun phase(): Phase? = phase

  override fun depends(): List<String> {
    // Combine phase dependencies with custom dependencies
    val phaseDeps = phase?.depends() ?: emptyList()
    return (phaseDeps + customDependencies).distinct()
  }

  /**
   * Returns the list of child task IDs that should be executed as part of this composite task.
   *
   * @return List of child task IDs
   */
  override fun children(): List<String> = children

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    val results = mutableListOf<TaskResult>()

    // Execute before hook if provided
    beforeChildren?.let { hook ->
      val beforeResult = hook(environment, projectContext)
      results.add(beforeResult)
      if (!beforeResult.success) {
        return TaskResult.failure(
          "Composite task '$id' failed in before-children hook: ${beforeResult.message}",
          results
        )
      }
    }

    // Child tasks will be resolved and executed by the TaskExecutor
    // This task just returns success, and the executor handles children
    // via the children() method

    // Execute after hook if provided
    afterChildren?.let { hook ->
      val afterResult = hook(environment, projectContext, results)
      results.add(afterResult)
      if (!afterResult.success) {
        return TaskResult.failure(
          "Composite task '$id' failed in after-children hook: ${afterResult.message}",
          results
        )
      }
      return afterResult
    }

    return TaskResult.success("Composite task '$id' completed", results)
  }
}
