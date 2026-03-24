package io.github.architectplatform.plugins.python

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class PythonPluginContractTest : ArchitectPluginContractTestSuite<PythonContext>() {

  override fun createPlugin() = PythonPlugin()

  override fun expectedTaskIds() = setOf(
    "py-install",
    "py-lint",
    "py-test",
    "py-build",
    "py-publish",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("py-install", "py-test")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
