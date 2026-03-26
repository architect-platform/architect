package io.github.architectplatform.api.core.logging

/**
 * Structured logging interface for tasks and plugins.
 *
 * Provides standard logging levels (debug, info, warn, error) with a consistent
 * API that tasks can use without depending on any specific logging framework.
 * The engine implementation delegates to SLF4J.
 *
 * Example usage:
 * ```kotlin
 * fun execute(environment: Environment, projectContext: ProjectContext): TaskResult {
 *   val logger = environment.logger("my-task")
 *   logger.info("Starting build for ${projectContext.dir}")
 *   logger.debug("Using config: ${config}")
 *
 *   return try {
 *     // ... do work ...
 *     logger.info("Build completed successfully")
 *     TaskResult.success("Done")
 *   } catch (e: Exception) {
 *     logger.error("Build failed: ${e.message}", e)
 *     TaskResult.failure("Build failed")
 *   }
 * }
 * ```
 */
interface ArchitectLogger {
  /** The tag/name identifying this logger instance. */
  val tag: String

  /** Log a debug-level message (only shown in verbose mode). */
  fun debug(message: String)

  /** Log an informational message (normal operation). */
  fun info(message: String)

  /** Log a warning message (non-fatal issue). */
  fun warn(message: String)

  /** Log an error message (failure condition). */
  fun error(message: String)

  /** Log an error message with an associated throwable. */
  fun error(
    message: String,
    throwable: Throwable,
  )
}
