package io.github.architectplatform.engine.core.tasks.interfaces

import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.micronaut.context.annotation.Context
import io.micronaut.runtime.event.annotation.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Simple event logger that outputs events to the console/logs.
 * 
 * Outputs events in a straightforward format showing event type, project context,
 * and relevant details.
 */
@Context
class EventLogger {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun onEvent(eventWrapper: ArchitectEvent<*>) {
    val event = eventWrapper.event

    if (event == null) {
      logger.warn("⚠️  Received null event (ID: ${eventWrapper.id})")
      return
    }

    if (event is ExecutionEvent) {
      val icon = when (event.executionEventType.toString().uppercase()) {
        "STARTED" -> "▶️"
        "COMPLETED" -> "✅"
        "FAILED" -> "❌"
        "SKIPPED" -> "⏭️"
        "OUTPUT" -> "📝"
        else -> "ℹ️"
      }

      val parts = mutableListOf<String>()
      parts.add("$icon ${event.executionEventType}")
      parts.add("[${event.project}]")
      event.subProject?.let { parts.add("(subproject: $it)") }
      event.message?.let { parts.add("- $it") }

      val logMessage = parts.joinToString(" ")
      logger.info(logMessage)

      // Log error details separately if present
      event.errorDetails?.let { errorDetails ->
        logger.error("❌ ERROR DETAILS:")
        errorDetails.lines().forEach { line ->
          logger.error("   $line")
        }
      }
    } else {
      // Generic event logging
      logger.info("ℹ️  Event ${eventWrapper.id}: $event")
    }
  }
}
