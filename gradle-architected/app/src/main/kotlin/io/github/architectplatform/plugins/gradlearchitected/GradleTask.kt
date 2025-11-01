package io.github.architectplatform.plugins.gradlearchitected

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import kotlin.io.path.Path

/**
 * Task implementation for executing Gradle commands.
 *
 * Executes a Gradle command across all configured Gradle projects,
 * aggregating results and handling conditional execution.
 *
 * @property command The Gradle command to execute (e.g., "build", "test")
 * @property phase The workflow phase this task belongs to
 * @property context The Gradle context containing project configurations
 * @property isEnabled Predicate to determine if task should run for a given project
 */
class GradleTask(
    private val command: String,
    private val phase: Phase,
    private val context: GradleContext,
    private val isEnabled: (GradleProjectContext) -> Boolean = { true }
) : Task {
  override val id: String = "gradle-$command"

  override fun phase(): Phase = phase

  /**
   * Executes the Gradle command for all configured projects.
   *
   * Iterates through all projects in the context, executing the Gradle command
   * for each one and aggregating the results.
   *
   * @param environment Execution environment providing CommandExecutor service
   * @param projectContext The project context
   * @param args Additional arguments to pass to the Gradle command
   * @return TaskResult indicating overall success or failure with sub-results
   */
  override fun execute(
      environment: Environment,
      projectContext: ProjectContext,
      args: List<String>
  ): TaskResult {
    val results =
        this.context.projects.map { singleProjectTask(environment, projectContext, args, it) }
    val success = results.all { it.success }
    if (!success) {
      return TaskResult.failure("Gradle task: $id failed for some projects", results = results)
    }
    return TaskResult.success(
        "Gradle task: $id executed successfully for all projects", results = results)
  }

  /**
   * Executes the Gradle command for a single project.
   *
   * Checks if the task is enabled for the project, constructs the command,
   * and executes it in the project's directory.
   *
   * @param environment Execution environment providing CommandExecutor service
   * @param projectContext The overall project context
   * @param args Additional arguments for the Gradle command
   * @param gradleProjectContext Configuration for the specific Gradle project
   * @return TaskResult indicating success or failure for this project
   */
  private fun singleProjectTask(
      environment: Environment,
      projectContext: ProjectContext,
      args: List<String>,
      gradleProjectContext: GradleProjectContext
  ): TaskResult {
    if (!isEnabled(gradleProjectContext)) {
      return TaskResult.success(
          "Gradle task: $id not enabled on gradle project: $gradleProjectContext. Skipping...")
    }
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val gradleProjectDir =
        Path(projectContext.dir.toString(), gradleProjectContext.path).toAbsolutePath()

    try {
      commandExecutor.execute(
          "${gradleProjectContext.gradlePath} $command ${args.joinToString(" ")}",
          workingDir = gradleProjectDir.toString())
    } catch (e: Exception) {
      return TaskResult.failure(
          "Gradle task: $id over gradle project: $gradleProjectContext failed with exception: ${e.message}")
    }

    return TaskResult.success(
        "Gradle task: $id over gradle project: $gradleProjectContext completed successfully")
  }
}
