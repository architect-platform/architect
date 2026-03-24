package io.github.architectplatform.plugins.kubernetes

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.utils.ShellUtils

class KubernetesPlugin : ArchitectPlugin<KubernetesContext> {
  override val id = "kubernetes-plugin"
  override val contextKey: String = "kubernetes"
  override val ctxClass: Class<KubernetesContext> = KubernetesContext::class.java
  override var context: KubernetesContext = KubernetesContext()

  private fun nsFlag(ctx: KubernetesContext): String =
    if (ctx.namespace.isNotEmpty()) " -n ${ShellUtils.escapeShellArg(ctx.namespace)}" else ""

  private fun ctxFlag(ctx: KubernetesContext): String =
    if (ctx.context.isNotEmpty()) " --context ${ShellUtils.escapeShellArg(ctx.context)}" else ""

  override fun register(registry: TaskRegistry) {
    registry.add(KubernetesTask(
      id = "k8s-apply",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { c, args ->
        val files = if (args.isNotEmpty()) args.joinToString(" -f ") { ShellUtils.escapeShellArg(it) } else ShellUtils.escapeShellArg(c.manifests)
        "kubectl apply${nsFlag(c)}${ctxFlag(c)} -f $files"
      },
    ))

    registry.add(KubernetesTask(
      id = "k8s-rollout",
      phase = CoreWorkflow.RUN,
      ctx = context,
      buildCommand = { c, args ->
        val resource = ShellUtils.escapeShellArg(if (args.isNotEmpty()) args[0] else "deployment")
        "kubectl rollout status${nsFlag(c)}${ctxFlag(c)} $resource"
      },
    ))

    registry.add(KubernetesTask(
      id = "k8s-status",
      phase = CoreWorkflow.VERIFY,
      ctx = context,
      buildCommand = { c, args ->
        val resources = if (args.isNotEmpty()) ShellUtils.escapeShellArgs(args) else ShellUtils.escapeShellArg("pods")
        "kubectl get${nsFlag(c)}${ctxFlag(c)} $resources"
      },
    ))

    registry.add(KubernetesTask(
      id = "k8s-port-forward",
      phase = CoreWorkflow.RUN,
      ctx = context,
      buildCommand = { c, args ->
        "kubectl port-forward${nsFlag(c)}${ctxFlag(c)} ${ShellUtils.escapeShellArgs(args)}"
      },
    ))
  }
}
