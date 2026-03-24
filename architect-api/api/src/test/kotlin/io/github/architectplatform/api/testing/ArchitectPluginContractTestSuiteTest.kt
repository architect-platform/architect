package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask

data class ContractTestSuiteContext(
  val message: String = "default",
)

class ArchitectPluginContractTestSuiteTest : ArchitectPluginContractTestSuite<ContractTestSuiteContext>() {

  override fun createPlugin(): ArchitectPlugin<ContractTestSuiteContext> = ContractPlugin()

  override fun expectedTaskIds(): Set<String> = setOf("contract-task")

  override fun pluginConfig(): Any = mapOf("message" to "hello")

  override fun executableTaskIds(): List<String> = listOf("contract-task")

  private class ContractPlugin : ArchitectPlugin<ContractTestSuiteContext> {
    override val id: String = "contract-plugin"
    override val contextKey: String = "contract"
    override val ctxClass: Class<ContractTestSuiteContext> = ContractTestSuiteContext::class.java
    override var context: ContractTestSuiteContext = ContractTestSuiteContext()

    override fun register(registry: TaskRegistry) {
      registry.add(
        SimpleTask(
          id = "contract-task",
          description = "Contract task",
        ) { _, _ ->
          TaskResult.success(context.message)
        }
      )
    }
  }
}