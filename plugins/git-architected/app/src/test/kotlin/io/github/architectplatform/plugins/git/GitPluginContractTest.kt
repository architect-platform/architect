package io.github.architectplatform.plugins.git

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class GitPluginContractTest : ArchitectPluginContractTestSuite<GitContext>() {

  override fun createPlugin() = GitPlugin()

  override fun expectedTaskIds() = setOf(
    "git-config",
    "git-remote",
    "git-status",
    "git-add",
    "git-commit",
    "git-pull",
    "git-fetch",
    "git-checkout",
    "git-branch",
    "git-log",
    "git-diff",
    "git-merge",
    "git-reset",
    "git-stash",
    "git-push",
    "git-tag",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("git-status", "git-push")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}