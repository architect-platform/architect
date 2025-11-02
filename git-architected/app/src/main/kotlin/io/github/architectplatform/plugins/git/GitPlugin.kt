package io.github.architectplatform.plugins.git

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Architect plugin for Git version control integration.
 *
 * Provides integration between Git commands and the Architect workflow system,
 * enabling execution of Git operations within Architect's structured workflow phases.
 *
 * Supports:
 * - Git configuration management (user.name, user.email, etc.)
 * - Proxying common Git commands through Architect
 * - Conventions over configuration approach
 * - Customizable through configuration files
 */
class GitPlugin : ArchitectPlugin<GitContext> {
  override val id = "git-plugin"
  override val contextKey: String = "git"
  override val ctxClass: Class<GitContext> = GitContext::class.java
  override var context: GitContext = GitContext()

  /**
   * Registers Git tasks with the task registry.
   *
   * Registered tasks include:
   * - git-config: Configure Git settings (INIT phase)
   * - git-status: Show working tree status
   * - git-add: Add file contents to the index
   * - git-commit: Record changes to the repository
   * - git-push: Update remote refs along with associated objects
   * - git-pull: Fetch from and integrate with another repository or local branch
   * - git-fetch: Download objects and refs from another repository
   * - git-checkout: Switch branches or restore working tree files
   * - git-branch: List, create, or delete branches
   * - git-log: Show commit logs
   * - git-diff: Show changes between commits, commit and working tree, etc
   * - git-merge: Join two or more development histories together
   * - git-reset: Reset current HEAD to the specified state
   * - git-stash: Stash the changes in a dirty working directory away
   * - git-tag: Create, list, delete or verify a tag object signed with GPG
   * - git-remote: Manage set of tracked repositories
   *
   * @param registry The task registry to add tasks to
   */
  override fun register(registry: TaskRegistry) {
    // Configuration task (INIT phase)
    registry.add(GitConfigTask(CodeWorkflow.INIT, context))

    // Common Git commands - these can be invoked by users as needed
    registry.add(GitTask("status", CodeWorkflow.BUILD, context))
    registry.add(GitTask("add", CodeWorkflow.BUILD, context))
    registry.add(GitTask("commit", CodeWorkflow.BUILD, context))
    registry.add(GitTask("push", CodeWorkflow.PUBLISH, context))
    registry.add(GitTask("pull", CodeWorkflow.BUILD, context))
    registry.add(GitTask("fetch", CodeWorkflow.BUILD, context))
    registry.add(GitTask("checkout", CodeWorkflow.BUILD, context))
    registry.add(GitTask("branch", CodeWorkflow.BUILD, context))
    registry.add(GitTask("log", CodeWorkflow.BUILD, context))
    registry.add(GitTask("diff", CodeWorkflow.BUILD, context))
    registry.add(GitTask("merge", CodeWorkflow.BUILD, context))
    registry.add(GitTask("reset", CodeWorkflow.BUILD, context))
    registry.add(GitTask("stash", CodeWorkflow.BUILD, context))
    registry.add(GitTask("tag", CodeWorkflow.BUILD, context))
    registry.add(GitTask("remote", CodeWorkflow.BUILD, context))
  }

  /**
   * Task for configuring Git settings.
   *
   * Applies all configured Git settings from the context's config map.
   */
  class GitConfigTask(private val phase: Phase, private val context: GitContext) : Task {
    override val id: String = "git-config"

    override fun phase(): Phase = phase

    /**
     * Executes Git configuration commands for all settings in the context.
     *
     * @param environment Execution environment providing CommandExecutor service
     * @param projectContext The project context
     * @param args Additional arguments (not used)
     * @return TaskResult indicating success or failure
     */
    override fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
      if (!context.enabled) {
        return TaskResult.success("Git configuration disabled. Skipping...")
      }

      if (context.config.isEmpty()) {
        return TaskResult.success(
            "No Git configuration specified. Using existing Git configuration.")
      }

      val commandExecutor = environment.service(CommandExecutor::class.java)
      val results = mutableListOf<TaskResult>()

      for ((key, value) in context.config) {
        try {
          // Validate config key to prevent command injection
          if (!GitUtils.isValidGitConfigKey(key)) {
            results.add(TaskResult.failure("Invalid Git config key: $key"))
            continue
          }
          
          // Note: CommandExecutor API only accepts a single command string.
          // We mitigate security risks through:
          // 1. Strict config key validation (isValidGitConfigKey)
          // 2. Shell argument escaping for values (escapeShellArg)
          // Since key is validated to contain only safe alphanumeric + dots, no escaping needed
          val escapedValue = GitUtils.escapeShellArg(value)
          
          commandExecutor.execute(
              "git config --local $key $escapedValue",
              workingDir = projectContext.dir.toString())
          results.add(TaskResult.success("Git config $key set to $value"))
        } catch (e: Exception) {
          results.add(
              TaskResult.failure(
                  "Failed to set Git config $key: ${e.message ?: "Unknown error"}"))
        }
      }

      val success = results.all { it.success }
      return if (success) {
        TaskResult.success("Git configuration completed successfully", results = results)
      } else {
        TaskResult.failure("Git configuration failed for some settings", results = results)
      }
    }
  }
}
