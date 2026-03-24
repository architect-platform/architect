package io.github.architectplatform.plugins.nx

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.utils.ShellUtils

/**
 * Bridges an Nx target to an Architect task.
 * Runs `npx nx run-many --target=<target>` (or `npx nx affected --target=<target>` when affected mode is on).
 */
class NxTask(
  override val id: String,
  private val nxTarget: String,
  private val phase: Phase,
  private val ctx: NxContext,
) : Task {
  private data class ExecutionOptions(
    val affected: Boolean,
    val projects: List<String>,
    val forwardedArgs: List<String>,
  )

  override fun phase(): Phase = phase

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!ctx.enabled) return TaskResult.success("Nx task $id disabled. Skipping...")
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val command = buildCommand(parseArgs(args))
    return try {
      commandExecutor.execute(command, workingDir = projectContext.dir.toString())
      TaskResult.success("Nx task $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure("Nx task $id failed: ${e.message ?: "Unknown error"}")
    }
  }

  private fun parseArgs(args: List<String>): ExecutionOptions {
    var affected = ctx.affected
    var projects = emptyList<String>()
    val forwardedArgs = mutableListOf<String>()

    args.forEach { arg ->
      when {
        arg == "--architect-affected" -> affected = true
        arg.startsWith("--architect-projects=") -> {
          projects = arg.substringAfter("=")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        }
        else -> forwardedArgs += arg
      }
    }

    return ExecutionOptions(affected = affected, projects = projects, forwardedArgs = forwardedArgs)
  }

  private fun buildCommand(options: ExecutionOptions): String = buildString {
    val safeTarget = ShellUtils.requireSafeIdentifier(nxTarget, "Nx target")
    append("npx nx")
    if (options.affected && options.projects.isNotEmpty()) {
      append(" run-many --target=$safeTarget")
      append(" --projects=${ShellUtils.escapeShellArg(options.projects.joinToString(","))}")
    } else if (options.affected) {
      append(" affected --target=$safeTarget")
    } else {
      append(" run-many --target=$safeTarget")
    }
    append(" --parallel=${ctx.parallel}")
    if (options.forwardedArgs.isNotEmpty()) {
      append(" ${ShellUtils.escapeShellArgs(options.forwardedArgs)}")
    }
  }
}
