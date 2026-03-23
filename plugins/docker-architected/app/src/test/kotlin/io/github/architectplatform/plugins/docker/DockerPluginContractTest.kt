package io.github.architectplatform.plugins.docker

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class DockerPluginContractTest : ArchitectPluginContractTestSuite<DockerContext>() {

  override fun createPlugin() = DockerPlugin()

  override fun expectedTaskIds() = setOf(
    "docker-build",
    "docker-push",
    "docker-run",
    "docker-compose-up",
    "docker-compose-down",
    "docker-compose-logs",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("docker-build", "docker-run")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}