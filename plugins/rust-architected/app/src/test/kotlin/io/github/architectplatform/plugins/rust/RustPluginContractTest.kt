package io.github.architectplatform.plugins.rust

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class RustPluginContractTest : ArchitectPluginContractTestSuite<RustContext>() {

  override fun createPlugin() = RustPlugin()

  override fun expectedTaskIds() = setOf(
    "cargo-build",
    "cargo-test",
    "cargo-lint",
    "cargo-publish",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("cargo-build", "cargo-test")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
