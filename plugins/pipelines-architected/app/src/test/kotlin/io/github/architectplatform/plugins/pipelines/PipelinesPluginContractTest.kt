package io.github.architectplatform.plugins.pipelines

import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class PipelinesPluginContractTest : ArchitectPluginContractTestSuite<PipelinesContext>() {

  override fun createPlugin() = PipelinesPlugin()

  override fun expectedTaskIds() = setOf(
    "pipelines-init",
    "pipelines-execute",
    "pipelines-list",
  )
}
