package io.github.architectplatform.plugins.rust

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class RustPlugin : ArchitectPlugin<RustContext> {
  override val id = "rust-plugin"
  override val contextKey: String = "rust"
  override val ctxClass: Class<RustContext> = RustContext::class.java
  override var context: RustContext = RustContext()

  private fun featureFlags(ctx: RustContext): String =
    if (ctx.features.isNotEmpty()) " --features ${ctx.features.joinToString(",")}" else ""

  private fun targetFlag(ctx: RustContext): String =
    if (ctx.target.isNotEmpty()) " --target ${ctx.target}" else ""

  override fun register(registry: TaskRegistry) {
    registry.add(RustTask(
      id = "cargo-build",
      phase = CoreWorkflow.BUILD,
      ctx = context,
      buildCommand = { ctx, args ->
        "cargo build --${ctx.profile}${featureFlags(ctx)}${targetFlag(ctx)}${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""}"
      },
    ))

    registry.add(RustTask(
      id = "cargo-test",
      phase = CoreWorkflow.TEST,
      ctx = context,
      buildCommand = { ctx, args ->
        "cargo test${featureFlags(ctx)}${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""}"
      },
    ))

    registry.add(RustTask(
      id = "cargo-lint",
      phase = CoreWorkflow.LINT,
      ctx = context,
      buildCommand = { ctx, args ->
        "cargo clippy${featureFlags(ctx)}${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else " -- -D warnings"}"
      },
    ))

    registry.add(RustTask(
      id = "cargo-publish",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { _, args ->
        "cargo publish${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""}"
      },
    ))
  }
}
