package io.github.architectplatform.plugins.maven

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer

class MavenPlugin : ArchitectPlugin<MavenContext> {
  override val id = "maven-plugin"
  override val contextKey: String = "maven"
  override val ctxClass: Class<MavenContext> = MavenContext::class.java
  override var context: MavenContext = MavenContext()

  private fun commonFlags(ctx: MavenContext): String = buildString {
    if (ctx.profiles.isNotEmpty()) append(" -P ${ShellArgumentSanitizer.escapeShellArg(ctx.profiles.joinToString(","))}")
    if (ctx.settings.isNotEmpty()) append(" -s ${ShellArgumentSanitizer.escapeShellArg(ctx.settings)}")
  }

  override fun register(registry: TaskRegistry) {
    registry.add(MavenTask(
      id = "mvn-verify",
      phase = CoreWorkflow.TEST,
      ctx = context,
      buildCommand = { ctx, args ->
        "mvn verify${commonFlags(ctx)}${if (args.isNotEmpty()) " ${ShellArgumentSanitizer.escapeShellArgs(args)}" else ""}"
      },
    ))

    registry.add(MavenTask(
      id = "mvn-package",
      phase = CoreWorkflow.BUILD,
      ctx = context,
      buildCommand = { ctx, args ->
        buildString {
          append("mvn package")
          if (ctx.skipTests) append(" -DskipTests")
          append(commonFlags(ctx))
          if (args.isNotEmpty()) append(" ${ShellArgumentSanitizer.escapeShellArgs(args)}")
        }
      },
    ))

    registry.add(MavenTask(
      id = "mvn-deploy",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { ctx, args ->
        buildString {
          append("mvn deploy")
          if (ctx.skipTests) append(" -DskipTests")
          append(commonFlags(ctx))
          if (args.isNotEmpty()) append(" ${ShellArgumentSanitizer.escapeShellArgs(args)}")
        }
      },
    ))
  }
}
