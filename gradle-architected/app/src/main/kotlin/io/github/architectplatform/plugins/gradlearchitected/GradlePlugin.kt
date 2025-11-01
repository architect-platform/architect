package io.github.architectplatform.plugins.gradlearchitected

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class GradlePlugin : ArchitectPlugin<GradleContext> {
  override val id = "gradle-plugin"
  override val contextKey: String = "gradle"
  override val ctxClass: Class<GradleContext> = GradleContext::class.java
  override var context: GradleContext = GradleContext()

  override fun register(registry: TaskRegistry) {
    // just "" because gradle tasks don't need a specific command to initialize
    registry.add(GradleTask("", CodeWorkflow.INIT, context))
    registry.add(GradleTask("build", CodeWorkflow.BUILD, context))
    registry.add(GradleTask("test", CodeWorkflow.TEST, context))
    registry.add(GradleTask("run", CodeWorkflow.RUN, context))
    registry.add(
        GradleTask(
            "publishGprPublicationToGitHubPackagesRepository", CodeWorkflow.PUBLISH, context) {
              it.githubPackageRelease
            })
  }
}
