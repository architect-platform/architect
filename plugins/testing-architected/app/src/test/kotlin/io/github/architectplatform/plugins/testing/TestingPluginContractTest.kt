package io.github.architectplatform.plugins.testing

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class TestingPluginContractTest : ArchitectPluginContractTestSuite<TestingContext>() {
  override fun createPlugin() = TestingPlugin()

  override fun expectedTaskIds() = setOf(
    "test-unit",
    "test-integration",
    "test-e2e",
    "test-coverage",
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
    ): CommandResult = CommandResult(exitCode = 0, stdout = "ok")
  }
}
