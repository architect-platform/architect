package io.github.architectplatform.plugins.release

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class ReleasePluginContractTest : ArchitectPluginContractTestSuite<ReleaseContext>() {
  override fun createPlugin() = ReleasePlugin()

  override fun expectedTaskIds() = setOf(
    "release-prepare",
    "release-publish",
    "release-rollback",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}

    override fun executeWithResult(
      command: String,
      workingDir: String?,
      timeoutSeconds: Long,
      env: Map<String, String>,
    ): CommandResult = CommandResult(exitCode = 0, stdout = "")
  }
}
