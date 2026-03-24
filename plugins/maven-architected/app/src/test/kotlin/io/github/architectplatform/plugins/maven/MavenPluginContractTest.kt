package io.github.architectplatform.plugins.maven

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class MavenPluginContractTest : ArchitectPluginContractTestSuite<MavenContext>() {

  override fun createPlugin() = MavenPlugin()

  override fun expectedTaskIds() = setOf(
    "mvn-verify",
    "mvn-package",
    "mvn-deploy",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("mvn-verify", "mvn-package")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
