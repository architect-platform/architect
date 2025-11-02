package io.github.architectplatform.plugins.git

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for GitContext data class.
 */
class GitContextTest {
  @Test
  fun `GitContext should have empty config by default`() {
    val context = GitContext()

    assertTrue(context.config.isEmpty())
    assertTrue(context.enabled)
  }

  @Test
  fun `GitContext should store multiple config values`() {
    val config = mapOf(
        "user.name" to "John Doe",
        "user.email" to "john@example.com",
        "core.editor" to "vim"
    )
    val context = GitContext(config = config)

    assertEquals(3, context.config.size)
    assertEquals("John Doe", context.config["user.name"])
    assertEquals("john@example.com", context.config["user.email"])
    assertEquals("vim", context.config["core.editor"])
  }

  @Test
  fun `GitContext can be disabled`() {
    val context = GitContext(enabled = false)

    assertFalse(context.enabled)
  }

  @Test
  fun `GitContext equality should work correctly`() {
    val config = mapOf("user.name" to "Test")
    val context1 = GitContext(config = config, enabled = true)
    val context2 = GitContext(config = config, enabled = true)

    assertEquals(context1, context2)
  }
}
