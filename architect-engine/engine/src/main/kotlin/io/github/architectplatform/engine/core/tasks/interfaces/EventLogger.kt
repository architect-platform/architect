package io.github.architectplatform.engine.core.tasks.interfaces

import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.micronaut.context.annotation.Context
import io.micronaut.runtime.event.annotation.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Context
class EventLogger {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  private fun createLogMessage(eventWrapper: ArchitectEvent<*>, icon: String? = null): String {
    val iconPrefix = icon ?: "ℹ️"
    val event = eventWrapper.event
    val serializedEvent = getSerializedFields(event)
    
    // Extract additional details if the event is an ExecutionEvent
    val additionalInfo = if (event is ExecutionEvent) {
      buildString {
        event.subProject?.let { append(" | SubProject: $it") }
        event.message?.let { append(" | Message: $it") }
      }
    } else ""
    
    return String.format(
        "%s ID: %-24s | Event: %s%s",
        iconPrefix,
        eventWrapper.id,
        serializedEvent,
        additionalInfo)
  }

  private fun logEvent(eventWrapper: ArchitectEvent<*>) {
    val event = eventWrapper.event

    if (event == null) {
      logger.warn("Received null event (ID: ${eventWrapper.id})")
      return
    }

    val icon: String? =
        when (event) {
          is ExecutionEvent -> {
            when (event.executionEventType.toString().uppercase()) {
              "STARTED" -> "▶️"
              "COMPLETED" -> "✅"
              "FAILED" -> "❌"
              "SKIPPED" -> "⏭️"
              "OUTPUT" -> "📝"
              else -> "ℹ️"
            }
          }
          else -> null
        }

    val logMessage = createLogMessage(eventWrapper, icon)
    logger.info(logMessage)
  }

  private fun getSerializedFields(event: Any?) =
      if (event == null) {
        "null"
      } else {
        // Use reflection to get all declared fields and their values
        event::class.java.declaredFields.joinToString(
            prefix = "[", postfix = "]", separator = ", ") { field ->
              field.isAccessible = true
              "${field.name}: ${field.get(event)}"
            }
      }

  @EventListener
  fun onEvent(eventWrapper: ArchitectEvent<*>) {
    logEvent(eventWrapper)
  }
}
