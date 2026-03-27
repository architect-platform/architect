package io.github.architectplatform.plugins.quality

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class QualityPluginContractTest : ArchitectPluginContractTestSuite<QualityContext>() {

  override fun createPlugin() = QualityPlugin()

  override fun expectedTaskIds() = setOf(
    "quality-lint",
    "quality-analyze",
    "quality-report",
    "quality-gate",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("quality-lint", "quality-gate")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
