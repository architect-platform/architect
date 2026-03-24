package io.github.architectplatform.plugins.javascript

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class JavaScriptPluginContractTest : ArchitectPluginContractTestSuite<JavaScriptContext>() {

  override fun createPlugin() = JavaScriptPlugin()

  override fun expectedTaskIds() = setOf(
    "javascript-install",
    "javascript-build",
    "javascript-test",
    "javascript-lint",
    "javascript-dev",
  )

  override fun services(): Map<Class<*>, Any> = mapOf(
    CommandExecutor::class.java to NoOpCommandExecutor(),
  )

  override fun executableTaskIds() = listOf("javascript-install", "javascript-build")

  private class NoOpCommandExecutor : CommandExecutor {
    override fun execute(command: String, workingDir: String?) {}
  }
}
