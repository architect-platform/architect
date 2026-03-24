package io.github.architectplatform.plugins.docker

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer

class DockerPlugin : ArchitectPlugin<DockerContext> {
  override val id = "docker-plugin"
  override val contextKey: String = "docker"
  override val ctxClass: Class<DockerContext> = DockerContext::class.java
  override var context: DockerContext = DockerContext()

  override fun register(registry: TaskRegistry) {
    registry.add(DockerTask(
      id = "docker-build",
      phase = CoreWorkflow.BUILD,
      context = context,
      buildCommand = { ctx, args, _ ->
        buildString {
          append("docker build")
          if (ctx.platforms.isNotEmpty()) {
            append(" --platform ${ShellArgumentSanitizer.escapeShellArg(ctx.platforms.joinToString(","))}")
          }
          ctx.buildArgs.forEach { (k, v) -> append(" --build-arg ${ShellArgumentSanitizer.escapeShellArg("$k=$v")}") }
          if (ctx.image.isNotEmpty()) append(" -t ${ShellArgumentSanitizer.escapeShellArg(ctx.image)}")
          append(" -f ${ShellArgumentSanitizer.escapeShellArg(ctx.dockerfile)}")
          if (args.isNotEmpty()) append(" ${ShellArgumentSanitizer.escapeShellArgs(args)}")
          append(" .")
        }
      },
    ))

    registry.add(DockerTask(
      id = "docker-push",
      phase = CoreWorkflow.PUBLISH,
      context = context,
      buildCommand = { ctx, args, _ ->
        val tag = if (args.isNotEmpty()) args[0] else ctx.image
        "docker push ${ShellArgumentSanitizer.escapeShellArg(tag)}"
      },
    ))

    registry.add(DockerTask(
      id = "docker-run",
      phase = CoreWorkflow.RUN,
      context = context,
      buildCommand = { ctx, args, _ ->
        buildString {
          append("docker run")
          if (args.isNotEmpty()) append(" ${ShellArgumentSanitizer.escapeShellArgs(args)}")
          else append(" ${ShellArgumentSanitizer.escapeShellArg(ctx.image)}")
        }
      },
    ))

    registry.add(DockerTask(
      id = "docker-compose-up",
      phase = CoreWorkflow.RUN,
      context = context,
      buildCommand = { ctx, args, _ ->
        buildString {
          append("docker compose -f ${ShellArgumentSanitizer.escapeShellArg(ctx.composeFile)} up -d")
          if (args.isNotEmpty()) append(" ${ShellArgumentSanitizer.escapeShellArgs(args)}")
        }
      },
    ))

    registry.add(DockerTask(
      id = "docker-compose-down",
      phase = CoreWorkflow.RUN,
      context = context,
      buildCommand = { ctx, _, _ ->
        "docker compose -f ${ShellArgumentSanitizer.escapeShellArg(ctx.composeFile)} down"
      },
    ))

    registry.add(DockerTask(
      id = "docker-compose-logs",
      phase = CoreWorkflow.RUN,
      context = context,
      buildCommand = { ctx, args, _ ->
        buildString {
          append("docker compose -f ${ShellArgumentSanitizer.escapeShellArg(ctx.composeFile)} logs")
          if (args.isNotEmpty()) append(" ${ShellArgumentSanitizer.escapeShellArgs(args)}")
        }
      },
    ))
  }
}
