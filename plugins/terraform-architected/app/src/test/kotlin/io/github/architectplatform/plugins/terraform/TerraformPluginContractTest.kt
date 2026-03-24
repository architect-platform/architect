package io.github.architectplatform.plugins.terraform

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class TerraformPluginContractTest : ArchitectPluginContractTestSuite<TerraformContext>() {

  override fun createPlugin() = TerraformPlugin()

  override fun expectedTaskIds() = setOf(
    "tf-init",
    "tf-plan",
    "tf-apply",
    "tf-destroy",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("tf-init", "tf-plan")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
