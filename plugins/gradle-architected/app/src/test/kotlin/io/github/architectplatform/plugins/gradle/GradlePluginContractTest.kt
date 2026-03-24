package io.github.architectplatform.plugins.gradle

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class GradlePluginContractTest : ArchitectPluginContractTestSuite<GradleContext>() {

  override fun createPlugin() = GradlePlugin()

  override fun expectedTaskIds() = setOf(
    "gradle-",
    "gradle-build",
    "gradle-test",
    "gradle-run",
    "gradle-publishGprPublicationToGitHubPackagesRepository",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("gradle-build", "gradle-test", "gradle-run")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
