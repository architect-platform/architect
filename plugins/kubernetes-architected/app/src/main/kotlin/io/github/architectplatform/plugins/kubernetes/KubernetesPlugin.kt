package io.github.architectplatform.plugins.kubernetes

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer

class KubernetesPlugin : ArchitectPlugin<KubernetesContext> {
  override val id = "kubernetes-plugin"
  override val contextKey: String = "kubernetes"
  override val ctxClass: Class<KubernetesContext> = KubernetesContext::class.java
  override var context: KubernetesContext = KubernetesContext()

  private fun nsFlag(ctx: KubernetesContext): String =
    if (ctx.namespace.isNotEmpty()) " -n ${ShellArgumentSanitizer.escapeShellArg(ctx.namespace)}" else ""

  private fun ctxFlag(ctx: KubernetesContext): String =
    if (ctx.context.isNotEmpty()) " --context ${ShellArgumentSanitizer.escapeShellArg(ctx.context)}" else ""

  override fun register(registry: TaskRegistry) {
    registry.add(KubernetesTask(
      id = "k8s-apply",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { c, args ->
        val files = if (args.isNotEmpty()) args.joinToString(" -f ") { ShellArgumentSanitizer.escapeShellArg(it) } else ShellArgumentSanitizer.escapeShellArg(c.manifests)
        "kubectl apply${nsFlag(c)}${ctxFlag(c)} -f $files"
      },
    ))

    registry.add(KubernetesTask(
      id = "k8s-rollout",
      phase = CoreWorkflow.RUN,
      ctx = context,
      buildCommand = { c, args ->
        val resource = ShellArgumentSanitizer.escapeShellArg(if (args.isNotEmpty()) args[0] else "deployment")
        "kubectl rollout status${nsFlag(c)}${ctxFlag(c)} $resource"
      },
    ))

    registry.add(KubernetesTask(
      id = "k8s-status",
      phase = CoreWorkflow.VERIFY,
      ctx = context,
      buildCommand = { c, args ->
        val resources = if (args.isNotEmpty()) ShellArgumentSanitizer.escapeShellArgs(args) else ShellArgumentSanitizer.escapeShellArg("pods")
        "kubectl get${nsFlag(c)}${ctxFlag(c)} $resources"
      },
    ))

    registry.add(KubernetesTask(
      id = "k8s-port-forward",
      phase = CoreWorkflow.RUN,
      ctx = context,
      buildCommand = { c, args ->
        "kubectl port-forward${nsFlag(c)}${ctxFlag(c)} ${ShellArgumentSanitizer.escapeShellArgs(args)}"
      },
    ))
  }
}
