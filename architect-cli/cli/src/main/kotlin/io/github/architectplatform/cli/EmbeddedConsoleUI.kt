package io.github.architectplatform.cli

import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.toTypedArchitectEvent

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

  val hasFailed: Boolean
    get() = delegate.hasFailed

  fun process(event: ArchitectEvent<*>) {
    val executionEvent = event.event as? ExecutionEvent ?: return
    val typedEvent =
      object : ArchitectEvent<ExecutionEvent> {
        override val id = event.id
        override val event = executionEvent
      }
    delegate.process(typedEvent.toTypedArchitectEvent())
  }

  fun complete(finalMessage: String) = delegate.complete(finalMessage)

  fun completeWithError(errorMessage: String) = delegate.completeWithError(errorMessage)
}
