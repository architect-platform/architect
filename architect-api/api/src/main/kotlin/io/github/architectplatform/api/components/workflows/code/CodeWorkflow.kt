package io.github.architectplatform.api.components.workflows.code

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Code asset sub-phases.
 *
 * CodeWorkflow provides specialized phases for code-based assets. Each phase in CodeWorkflow
 * corresponds to a parent phase in CoreWorkflow, allowing for more fine-grained control
 * and organization of code-specific tasks.
 *
 * The phases follow the same lifecycle as CoreWorkflow but are prefixed with "CODE-"
 * to distinguish them as code-specific phases.
 *
 * Example usage:
 * ```kotlin
 * val task = SimpleTask(
 *   id = "typescript-compile",
 *   description = "Compiles TypeScript code",
 *   phase = CodeWorkflow.BUILD
 * ) { env, ctx ->
 *   // Task implementation
 *   TaskResult.success()
 * }
 * ```
 */
enum class CodeWorkflow(
  private val parent: CoreWorkflow,
  override val id: String = "CODE-" + parent.id,
) : Phase {
  /** Code initialization phase - maps to CoreWorkflow.INIT */
  INIT(CoreWorkflow.INIT),

  /** Code linting phase - maps to CoreWorkflow.LINT */
  LINT(CoreWorkflow.LINT),

  /** Code verification phase - maps to CoreWorkflow.VERIFY */
  VERIFY(CoreWorkflow.VERIFY),

  /** Code build phase - maps to CoreWorkflow.BUILD */
  BUILD(CoreWorkflow.BUILD),

  /** Code test phase - maps to CoreWorkflow.TEST */
  TEST(CoreWorkflow.TEST),

  /** Code run phase - maps to CoreWorkflow.RUN */
  RUN(CoreWorkflow.RUN),

  /** Code release phase - maps to CoreWorkflow.RELEASE */
  RELEASE(CoreWorkflow.RELEASE),

  /** Code publish phase - maps to CoreWorkflow.PUBLISH */
  PUBLISH(CoreWorkflow.PUBLISH),
  ;

  override fun description(): String {
    return "Code workflow phase for ${parent.id} - $id"
  }

  override fun parent(): Phase {
    return parent
  }
}
