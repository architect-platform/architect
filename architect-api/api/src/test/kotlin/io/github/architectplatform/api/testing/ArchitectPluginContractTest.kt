package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ArchitectPluginContractTest {

  @Test
  fun `verify passes for compliant plugin`() {
    ArchitectPluginContract { ContractPlugin() }
      .verify(
        ArchitectPluginContract.Verification(
          config = mapOf("message" to "hello"),
          expectedTaskIds = setOf("contract-task"),
          executableTaskIds = listOf("contract-task"),
        )
      )
  }

  @Test
  fun `verify fails for blank plugin id`() {
    val error = assertFailsWith<IllegalArgumentException> {
      ArchitectPluginContract { BlankIdPlugin() }.verify()
    }

    kotlin.test.assertTrue(error.message.orEmpty().contains("id must not be blank"))
  }

  @Test
  fun `verify fails when expected task is missing`() {
    val error = assertFailsWith<IllegalArgumentException> {
      ArchitectPluginContract { ContractPlugin() }
        .verify(ArchitectPluginContract.Verification(expectedTaskIds = setOf("missing-task")))
    }

    kotlin.test.assertTrue(error.message.orEmpty().contains("missing expected tasks"))
  }

  data class ContractContext(
    val message: String = "default",
  )

  private class ContractPlugin : ArchitectPlugin<ContractContext> {
    override val id: String = "contract-plugin"
    override val contextKey: String = "contract"
    override val ctxClass: Class<ContractContext> = ContractContext::class.java
    override var context: ContractContext = ContractContext()

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

  private class BlankIdPlugin : ArchitectPlugin<Unit> {
    override val id: String = ""
    override val contextKey: String = "blank"
    override val ctxClass: Class<Unit> = Unit::class.java
    override var context: Unit = Unit

    override fun register(registry: TaskRegistry) {
      registry.add(
        SimpleTask(
          id = "blank-task",
          description = "Blank plugin task",
        ) { _, _ ->
          TaskResult.success("ok")
        }
      )
    }
  }
}