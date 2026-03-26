package io.github.architectplatform.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.core.domain.events.ArchitectEvent

/**
 * Console renderer for embedded mode with the same output contract as ConsoleUI.
 */
class EmbeddedConsoleUI(
  taskName: String,
  plain: Boolean = false,
  verbosity: Int = 1,
  timing: Boolean = false,
) {
  private val delegate = ConsoleUI(taskName, plain, verbosity, timing)
  private val objectMapper = ObjectMapper().registerKotlinModule()

  val hasFailed: Boolean
    get() = delegate.hasFailed

  fun process(event: ArchitectEvent<*>) {
    val eventMap = objectMapper.convertValue<Map<String, Any>>(event)
    delegate.process(eventMap)
  }

  fun complete(finalMessage: String) = delegate.complete(finalMessage)

  fun completeWithError(errorMessage: String) = delegate.completeWithError(errorMessage)
}
