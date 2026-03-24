package io.github.architectplatform.plugins.github

import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite
import io.github.architectplatform.plugins.github.dto.GithubContext

class GithubPluginContractTest : ArchitectPluginContractTestSuite<GithubContext>() {

  override fun createPlugin() = GithubPlugin()

  override fun expectedTaskIds() = setOf(
    "github-release-task",
    "github-init-pipelines",
    "github-init-dependencies",
  )
}
