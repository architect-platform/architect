package io.github.architectplatform.plugins.nx

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class NxPluginContractTest : ArchitectPluginContractTestSuite<NxContext>() {

  override fun createPlugin() = NxPlugin()

  override fun expectedTaskIds() = setOf(
    "nx-build",
    "nx-test",
    "nx-lint",
    "nx-e2e",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("nx-build", "nx-test")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
