package io.github.architectplatform.plugins.nx

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Nx monorepo integration plugin.
 *
 * Exposes configured Nx targets as Architect tasks and bridges affectedness detection
 * so that `architect affected` and `nx affected` work together.
 * By default, exposes the standard targets (build, test, lint, e2e).
 * Custom targets can be added via context configuration.
 */
class NxPlugin : ArchitectPlugin<NxContext> {
  override val id = "nx-plugin"
  override val contextKey: String = "nx"
  override val ctxClass: Class<NxContext> = NxContext::class.java
  override var context: NxContext = NxContext()

  /** Maps standard Nx target names to Architect phases. */
  private val defaultTargetPhases = mapOf(
    "build" to CoreWorkflow.BUILD,
    "test" to CoreWorkflow.TEST,
    "lint" to CoreWorkflow.LINT,
    "e2e" to CoreWorkflow.TEST,
  )

  override fun register(registry: TaskRegistry) {
    // Register default targets
    defaultTargetPhases.forEach { (target, phase) ->
      registry.add(NxTask(
        id = "nx-$target",
        nxTarget = target,
        phase = phase,
        ctx = context,
      ))
    }

    // Register any additional custom targets from context
    context.targets
      .filter { it !in defaultTargetPhases }
      .forEach { target ->
        registry.add(NxTask(
          id = "nx-$target",
          nxTarget = target,
          phase = CoreWorkflow.RUN,
          ctx = context,
        ))
      }
  }
}
