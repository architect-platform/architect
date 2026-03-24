package io.github.architectplatform.plugins.docs

import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite
import io.github.architectplatform.plugins.docs.dto.DocsContext

class DocsPluginContractTest : ArchitectPluginContractTestSuite<DocsContext>() {

  override fun createPlugin() = DocsPlugin()

  override fun expectedTaskIds() = setOf(
    "docs-init",
    "docs-build",
    "docs-publish",
  )
}
