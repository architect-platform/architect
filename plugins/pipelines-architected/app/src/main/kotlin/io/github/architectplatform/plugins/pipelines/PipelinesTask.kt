package io.github.architectplatform.plugins.pipelines

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

class PipelinesTask(
    override val id: String,
    private val phase: Phase,
    private val task: (Environment, ProjectContext, List<String>) -> TaskResult
) : Task {
  override fun phase(): Phase = phase

  override fun execute(
      environment: Environment,
      projectContext: ProjectContext,
      args: List<String>
  ): TaskResult {
    return try {
      task(environment, projectContext, args)
    } catch (e: Exception) {
      TaskResult.failure(
          "Pipelines task: $id failed with exception: ${e.message ?: "Unknown error"}")
    }
  }
}
