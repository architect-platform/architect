package io.github.architectplatform.api.core.tasks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Environment interface default implementations.
 */
class EnvironmentTest {
  private val defaultEnv =
    object : Environment {
      override fun <T> service(type: Class<T>): T =
        throw UnsupportedOperationException("Not implemented")

      override fun publish(event: Any) {}
    }

  @Test
  fun `variable returns system env var when set`() {
    // PATH is always set on any OS
    val path = defaultEnv.variable("PATH")
    assertNotNull(path)
    assertTrue(path!!.isNotEmpty())
  }

  @Test
  fun `variable returns null for unset variable`() {
    val result = defaultEnv.variable("ARCHITECT_DEFINITELY_NOT_SET_12345")
    assertNull(result)
  }

  @Test
  fun `profile returns default when not overridden`() {
    assertEquals("default", defaultEnv.profile())
  }

  @Test
  fun `secret returns null when not overridden`() {
    assertNull(defaultEnv.secret("any-secret"))
  }

  @Test
  fun `logger returns no-op logger by default`() {
    val logger = defaultEnv.logger("test")
    assertEquals("test", logger.tag)
    // Should not throw
    logger.info("test message")
    logger.debug("debug message")
    logger.warn("warn message")
    logger.error("error message")
    logger.error("error with exception", RuntimeException("test"))
  }

  @Test
  fun `subscribe is no-op by default`() {
    // Should not throw
    defaultEnv.subscribe(String::class.java) { _ -> }
  }

  @Test
  fun `progressReporter returns NOOP by default`() {
    val reporter = defaultEnv.progressReporter()
    assertNotNull(reporter)
    // Should not throw
    reporter.report(1, 10, "step 1")
    reporter.report(10, 10, "done")
  }
}
