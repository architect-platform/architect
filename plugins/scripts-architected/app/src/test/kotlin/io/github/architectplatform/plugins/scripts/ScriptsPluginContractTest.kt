package io.github.architectplatform.plugins.scripts

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class ScriptsPluginContractTest : ArchitectPluginContractTestSuite<ScriptsContext>() {

  override fun createPlugin() = ScriptsPlugin()

  override fun expectedTaskIds() = setOf("scripts-lint", "scripts-release")

  override fun pluginConfig(): Any = ScriptsContext(
    scripts = mapOf(
      "lint" to ScriptConfig(
        command = "./scripts/lint.sh",
        description = "Lint sources",
        phase = "LINT",
      ),
      "release" to ScriptConfig(
        command = "./scripts/release.sh",
        description = "Release build",
        phase = "PUBLISH",
      ),
    )
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("scripts-lint", "scripts-release")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}