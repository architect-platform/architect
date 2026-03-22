package io.github.architectplatform.plugins.go

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

class GoTask(
  override val id: String,
  private val phase: Phase,
  private val ctx: GoContext,
  private val buildCommand: (GoContext, List<String>) -> String,
) : Task {
  override fun phase(): Phase = phase

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!ctx.enabled) return TaskResult.success("Go task $id disabled. Skipping...")
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val command = buildCommand(ctx, args)
    return try {
      commandExecutor.execute(command, workingDir = projectContext.dir.toString())
      TaskResult.success("Go task $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure("Go task $id failed: ${e.message ?: "Unknown error"}")
    }
  }
}
