package io.github.architectplatform.plugins.go

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class GoPluginContractTest : ArchitectPluginContractTestSuite<GoContext>() {

  override fun createPlugin() = GoPlugin()

  override fun expectedTaskIds() = setOf(
    "go-build",
    "go-test",
    "go-lint",
    "go-release",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("go-build", "go-test")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
