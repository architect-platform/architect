package io.github.architectplatform.api.components.workflows.hooks

import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Git hooks workflow phases.
 *
 * HooksWorkflow defines phases corresponding to Git hooks, allowing tasks to be executed
 * at specific points in the Git workflow. These phases are independent of the CoreWorkflow
 * and don't have parent phases.
 *
 * Common use cases:
 * - PRE_COMMIT: Run linters, formatters, or quick tests before committing
 * - PRE_PUSH: Run full test suites or builds before pushing
 * - COMMIT_MSG: Validate or format commit messages
 *
 * Example usage:
 * ```kotlin
 * val task = SimpleTask(
 *   id = "format-code",
 *   description = "Formats code before commit",
 *   phase = HooksWorkflow.PRE_COMMIT
 * ) { env, ctx ->
 *   // Task implementation
 *   TaskResult.success()
 * }
 * ```
 */
enum class HooksWorkflow(override val id: String) : Phase {
  /** Pre-commit hook - runs before a commit is created */
  PRE_COMMIT("pre-commit"),

  /** Pre-push hook - runs before changes are pushed to remote */
  PRE_PUSH("pre-push"),

  /** Commit message hook - runs to validate or modify commit messages */
  COMMIT_MSG("commit-msg"),
  ;

  override fun parent(): Phase? {
    return null // HooksWorkflow does not have a parent phase
  }
}
