package io.github.architectplatform.api.core.tasks.impl

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * A simple task implementation that executes a lambda function without arguments.
 *
 * SimpleTask provides a convenient way to create tasks that don't require command-line
 * arguments. The task logic is provided as a lambda function that receives the environment
 * and project context.
 *
 * This task supports:
 * - Optional phase membership (can be standalone or belong to a phase)
 * - Custom dependencies beyond phase dependencies
 * - Convention over configuration with sensible defaults
 *
 * Example usage:
 * ```kotlin
 * // Task with phase
 * val task = SimpleTask(
 *   id = "hello",
 *   description = "Prints hello",
 *   phase = CoreWorkflow.INIT
 * ) { env, ctx ->
 *   println("Hello from ${ctx.dir}")
 *   TaskResult.success()
 * }
 *
 * // Standalone task with custom dependencies
 * val task = SimpleTask(
 *   id = "verify",
 *   description = "Verify setup",
 *   customDependencies = listOf("init", "configure")
 * ) { env, ctx ->
 *   // Verification logic
 *   TaskResult.success()
 * }
 * ```
 *
 * @property id Unique identifier for this task
 * @param description Human-readable description of what this task does
 * @param phase The lifecycle phase this task belongs to (optional, null for standalone tasks)
 * @param customDependencies Additional dependencies beyond phase dependencies (optional)
 * @param task Lambda function containing the task logic
 */
class SimpleTask(
  override val id: String,
  private val description: String,
  private val phase: Phase? = null,
  private val customDependencies: List<String> = emptyList(),
  private val task: (Environment, ProjectContext) -> TaskResult,
) : Task {
  override fun phase(): Phase? = phase

  override fun description(): String = description

  override fun depends(): List<String> {
    // Combine phase dependencies with custom dependencies
    val phaseDeps = phase?.depends() ?: emptyList()
    return (phaseDeps + customDependencies).distinct()
  }

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    return task(environment, projectContext)
  }
}
