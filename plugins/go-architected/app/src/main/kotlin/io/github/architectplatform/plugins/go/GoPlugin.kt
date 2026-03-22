package io.github.architectplatform.plugins.go

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class GoPlugin : ArchitectPlugin<GoContext> {
  override val id = "go-plugin"
  override val contextKey: String = "go"
  override val ctxClass: Class<GoContext> = GoContext::class.java
  override var context: GoContext = GoContext()

  override fun register(registry: TaskRegistry) {
    registry.add(GoTask(
      id = "go-build",
      phase = CoreWorkflow.BUILD,
      ctx = context,
      buildCommand = { ctx, args ->
        buildString {
          append("go build")
          if (ctx.ldflags.isNotEmpty()) append(" -ldflags '${ctx.ldflags}'")
          if (ctx.outputBinary.isNotEmpty()) append(" -o ${ctx.outputBinary}")
          if (args.isNotEmpty()) append(" ${args.joinToString(" ")}")
          else append(" ./...")
        }
      },
    ))

    registry.add(GoTask(
      id = "go-test",
      phase = CoreWorkflow.TEST,
      ctx = context,
      buildCommand = { _, args ->
        "go test${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else " ./..."}"
      },
    ))

    registry.add(GoTask(
      id = "go-lint",
      phase = CoreWorkflow.LINT,
      ctx = context,
      buildCommand = { _, args ->
        "golangci-lint run${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""}"
      },
    ))

    registry.add(GoTask(
      id = "go-release",
      phase = CoreWorkflow.RELEASE,
      ctx = context,
      buildCommand = { _, args ->
        "goreleaser release${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else " --clean"}"
      },
    ))
  }
}
