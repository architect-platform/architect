package io.github.architectplatform.plugins.security

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class SecurityPluginContractTest : ArchitectPluginContractTestSuite<SecurityContext>() {
  override fun createPlugin() = SecurityPlugin()

  override fun expectedTaskIds() = setOf(
    "security-scan",
    "security-audit",
    "security-sbom",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}

    override fun executeWithResult(
      command: String,
      workingDir: String?,
      timeoutSeconds: Long,
      env: Map<String, String>,
    ): CommandResult = CommandResult(exitCode = 0, stdout = "{}")
  }
}
