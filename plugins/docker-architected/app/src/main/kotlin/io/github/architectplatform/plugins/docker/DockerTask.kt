package io.github.architectplatform.plugins.docker

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

class DockerTask(
  override val id: String,
  private val phase: Phase,
  private val context: DockerContext,
  private val buildCommand: (DockerContext, List<String>, String) -> String,
) : Task {
  override fun phase(): Phase = phase

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.success("Docker task $id disabled. Skipping...")
    }
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val workDir = projectContext.dir.toString()
    val command = buildCommand(context, args, workDir)
    return try {
      commandExecutor.execute(command, workingDir = workDir)
      TaskResult.success("Docker task $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure("Docker task $id failed: ${e.message ?: "Unknown error"}")
    }
  }
}
