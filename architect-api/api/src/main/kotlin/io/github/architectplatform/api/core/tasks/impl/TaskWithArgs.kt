package io.github.architectplatform.api.core.tasks.impl

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * A task implementation that accepts and processes command-line arguments.
 *
 * TaskWithArgs provides a convenient way to create tasks that need to handle command-line
 * arguments. The task logic is provided as a lambda function that receives the environment,
 * project context, and the argument list.
 *
 * This task supports:
 * - Optional phase membership (can be standalone or belong to a phase)
 * - Custom dependencies beyond phase dependencies
 * - Flexible argument handling
 * - Convention over configuration with sensible defaults
 *
 * Example usage:
 * ```kotlin
 * // Task with phase
 * val task = TaskWithArgs(
 *   id = "greet",
 *   description = "Greets a person by name",
 *   phase = CoreWorkflow.RUN
 * ) { env, ctx, args ->
 *   val name = args.firstOrNull() ?: "World"
 *   println("Hello, $name!")
 *   TaskResult.success()
 * }
 *
 * // Standalone task with dependencies
 * val task = TaskWithArgs(
 *   id = "deploy",
 *   description = "Deploy to environment",
 *   customDependencies = listOf("build", "test")
 * ) { env, ctx, args ->
 *   val environment = args.firstOrNull() ?: "staging"
 *   // Deploy logic
 *   TaskResult.success("Deployed to $environment")
 * }
 * ```
 *
 * @property id Unique identifier for this task
 * @param description Human-readable description of what this task does
 * @param phase The lifecycle phase this task belongs to (optional, null for standalone tasks)
 * @param customDependencies Additional dependencies beyond phase dependencies (optional)
 * @param task Lambda function containing the task logic, receives arguments as a list
 */
class TaskWithArgs(
  override val id: String,
  val description: String,
  private val phase: Phase? = null,
  private val customDependencies: List<String> = emptyList(),
  private val task: (Environment, ProjectContext, List<String>) -> TaskResult,
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
    return task(environment, projectContext, args)
  }
}
