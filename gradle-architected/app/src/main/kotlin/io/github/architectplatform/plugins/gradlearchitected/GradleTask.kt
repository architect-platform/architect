package io.github.architectplatform.plugins.gradlearchitected

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import kotlin.io.path.Path

class GradleTask(
    private val command: String,
    private val phase: Phase,
    private val context: GradleContext,
    private val isEnabled: (GradleProjectContext) -> Boolean = { true }
) : Task {
  override val id: String = "gradle-$command"

  override fun phase(): Phase = phase

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
