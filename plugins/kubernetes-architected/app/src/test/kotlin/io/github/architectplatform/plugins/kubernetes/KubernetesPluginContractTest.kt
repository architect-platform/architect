package io.github.architectplatform.plugins.kubernetes

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class KubernetesPluginContractTest : ArchitectPluginContractTestSuite<KubernetesContext>() {

  override fun createPlugin() = KubernetesPlugin()

  override fun expectedTaskIds() = setOf(
    "k8s-apply",
    "k8s-rollout",
    "k8s-status",
    "k8s-port-forward",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("k8s-apply", "k8s-status")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
