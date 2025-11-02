package io.github.architectplatform.plugins.docs.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for PublishContext data class.
 */
class PublishContextTest {
  @Test
  fun `PublishContext should have default values`() {
    val context = PublishContext()

    assertTrue(context.enabled)
    assertTrue(context.githubPages)
    assertEquals("gh-pages", context.branch)
    assertEquals("", context.domain)
    assertTrue(context.cname)
  }

  @Test
  fun `PublishContext should accept custom values`() {
    val context =
        PublishContext(
            enabled = false,
            githubPages = false,
            branch = "docs",
            domain = "docs.example.com",
            cname = false)

    assertFalse(context.enabled)
    assertFalse(context.githubPages)
    assertEquals("docs", context.branch)
    assertEquals("docs.example.com", context.domain)
    assertFalse(context.cname)
  }

  @Test
  fun `PublishContext equality should work correctly`() {
    val context1 = PublishContext(domain = "example.com")
    val context2 = PublishContext(domain = "example.com")

    assertEquals(context1, context2)
  }

  @Test
  fun `PublishContext should support custom domain configuration`() {
    val withDomain = PublishContext(domain = "docs.myproject.com", cname = true)
    val withoutDomain = PublishContext(domain = "", cname = false)

    assertEquals("docs.myproject.com", withDomain.domain)
    assertTrue(withDomain.cname)

    assertEquals("", withoutDomain.domain)
    assertFalse(withoutDomain.cname)
  }
}
