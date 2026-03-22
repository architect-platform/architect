package io.github.architectplatform.plugins.terraform

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class TerraformPlugin : ArchitectPlugin<TerraformContext> {
  override val id = "terraform-plugin"
  override val contextKey: String = "terraform"
  override val ctxClass: Class<TerraformContext> = TerraformContext::class.java
  override var context: TerraformContext = TerraformContext()

  private fun varFlags(ctx: TerraformContext): String = buildString {
    ctx.vars.forEach { (k, v) -> append(" -var '$k=$v'") }
    if (ctx.varFile.isNotEmpty()) append(" -var-file=${ctx.varFile}")
  }

  override fun register(registry: TaskRegistry) {
    registry.add(TerraformTask(
      id = "tf-init",
      phase = CoreWorkflow.INIT,
      ctx = context,
      buildCommand = { ctx, args ->
        buildString {
          append("terraform init")
          if (ctx.backend.isNotEmpty()) append(" -backend-config=${ctx.backend}")
          if (args.isNotEmpty()) append(" ${args.joinToString(" ")}")
        }
      },
    ))

    registry.add(TerraformTask(
      id = "tf-plan",
      phase = CoreWorkflow.VERIFY,
      ctx = context,
      buildCommand = { ctx, args ->
        "terraform plan${varFlags(ctx)}${if (args.isNotEmpty()) " ${args.joinToString(" ")}" else ""}"
      },
    ))

    registry.add(TerraformTask(
      id = "tf-apply",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { ctx, args ->
        buildString {
          append("terraform apply")
          if (ctx.autoApprove) append(" -auto-approve")
          append(varFlags(ctx))
          if (args.isNotEmpty()) append(" ${args.joinToString(" ")}")
        }
      },
    ))

    registry.add(TerraformTask(
      id = "tf-destroy",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { ctx, args ->
        buildString {
          append("terraform destroy")
          if (ctx.autoApprove) append(" -auto-approve")
          append(varFlags(ctx))
          if (args.isNotEmpty()) append(" ${args.joinToString(" ")}")
        }
      },
    ))
  }
}
