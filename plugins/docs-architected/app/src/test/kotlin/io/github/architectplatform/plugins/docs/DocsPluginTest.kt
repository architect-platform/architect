package io.github.architectplatform.plugins.docs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for DocsPlugin utility functions.
 */
class DocsPluginTest {

  @Test
  fun `isValidDomain should accept valid domains`() {
    assertTrue(DocsPlugin.isValidDomain("example.com"))
    assertTrue(DocsPlugin.isValidDomain("docs.example.com"))
    assertTrue(DocsPlugin.isValidDomain("my-site.github.io"))
    assertTrue(DocsPlugin.isValidDomain("sub.domain.example.com"))
  }

  @Test
  fun `isValidDomain should reject invalid domains`() {
    assertFalse(DocsPlugin.isValidDomain("-example.com"))
    assertFalse(DocsPlugin.isValidDomain(".example.com"))
    assertFalse(DocsPlugin.isValidDomain(""))
  }

  @Test
  fun `isValidDomain should reject domain with special characters`() {
    assertFalse(DocsPlugin.isValidDomain("example.com;ls"))
    assertFalse(DocsPlugin.isValidDomain("example.com/path"))
    assertFalse(DocsPlugin.isValidDomain("example.com:8080"))
  }
}
