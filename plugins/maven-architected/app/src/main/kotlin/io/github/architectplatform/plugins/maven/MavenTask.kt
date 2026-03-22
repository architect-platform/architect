package io.github.architectplatform.plugins.maven

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

class MavenTask(
  override val id: String,
  private val phase: Phase,
  private val ctx: MavenContext,
  private val buildCommand: (MavenContext, List<String>) -> String,
) : Task {
  override fun phase(): Phase = phase

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!ctx.enabled) return TaskResult.success("Maven task $id disabled. Skipping...")
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val command = buildCommand(ctx, args)
    return try {
      commandExecutor.execute(command, workingDir = projectContext.dir.toString())
      TaskResult.success("Maven task $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure("Maven task $id failed: ${e.message ?: "Unknown error"}")
    }
  }
}
