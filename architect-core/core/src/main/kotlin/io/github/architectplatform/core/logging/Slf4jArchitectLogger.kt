package io.github.architectplatform.core.logging

import io.github.architectplatform.api.core.logging.ArchitectLogger
import org.slf4j.LoggerFactory

/**
 * SLF4J-backed implementation of [ArchitectLogger].
 *
 * Delegates all log calls to a SLF4J logger named "architect.task.<tag>",
 * providing structured, framework-standard logging for task execution.
 */
class Slf4jArchitectLogger(override val tag: String) : ArchitectLogger {
  private val delegate = LoggerFactory.getLogger("architect.task.$tag")

  override fun debug(message: String) {
    delegate.debug("[{}] {}", tag, message)
  }

  override fun info(message: String) {
    delegate.info("[{}] {}", tag, message)
  }

  override fun warn(message: String) {
    delegate.warn("[{}] {}", tag, message)
  }

  override fun error(message: String) {
    delegate.error("[{}] {}", tag, message)
  }

  override fun error(
    message: String,
    throwable: Throwable,
  ) {
    delegate.error("[{}] {}", tag, message, throwable)
  }
}
