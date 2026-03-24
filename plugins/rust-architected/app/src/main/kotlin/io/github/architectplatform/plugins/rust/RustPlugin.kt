package io.github.architectplatform.plugins.rust

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.utils.ShellUtils

class RustPlugin : ArchitectPlugin<RustContext> {
  override val id = "rust-plugin"
  override val contextKey: String = "rust"
  override val ctxClass: Class<RustContext> = RustContext::class.java
  override var context: RustContext = RustContext()

  private fun featureFlags(ctx: RustContext): String =
    if (ctx.features.isNotEmpty()) " --features ${ShellUtils.escapeShellArg(ctx.features.joinToString(","))}" else ""

  private fun targetFlag(ctx: RustContext): String =
    if (ctx.target.isNotEmpty()) " --target ${ShellUtils.escapeShellArg(ctx.target)}" else ""

  override fun register(registry: TaskRegistry) {
    registry.add(RustTask(
      id = "cargo-build",
      phase = CoreWorkflow.BUILD,
      ctx = context,
      buildCommand = { ctx, args ->
        val safeProfile = ShellUtils.requireSafeIdentifier(ctx.profile, "Rust build profile")
        "cargo build --$safeProfile${featureFlags(ctx)}${targetFlag(ctx)}${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
      },
    ))

    registry.add(RustTask(
      id = "cargo-test",
      phase = CoreWorkflow.TEST,
      ctx = context,
      buildCommand = { ctx, args ->
        "cargo test${featureFlags(ctx)}${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
      },
    ))

    registry.add(RustTask(
      id = "cargo-lint",
      phase = CoreWorkflow.LINT,
      ctx = context,
      buildCommand = { ctx, args ->
        "cargo clippy${featureFlags(ctx)}${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else " -- -D warnings"}"
      },
    ))

    registry.add(RustTask(
      id = "cargo-publish",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { _, args ->
        "cargo publish${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
      },
    ))
  }
}
