package io.github.architectplatform.plugins.docs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for DocsPlugin utility functions.
 */
class DocsPluginTest {
  @Test
  fun `sanitizePath should remove dangerous characters`() {
    assertEquals("docs", DocsPlugin.sanitizePath("docs"))
    assertEquals("docs/guide", DocsPlugin.sanitizePath("docs/guide"))
    assertEquals("buildsite", DocsPlugin.sanitizePath("build/site"))
    assertEquals("", DocsPlugin.sanitizePath("docs;rm -rf /"))
    assertEquals("testpath", DocsPlugin.sanitizePath("test\$path"))
  }

  @Test
  fun `sanitizePath should prevent directory traversal`() {
    assertEquals("", DocsPlugin.sanitizePath("/etc/passwd"))
    assertEquals("docsbuild", DocsPlugin.sanitizePath("../docs/build"))
    assertEquals("docsbuild", DocsPlugin.sanitizePath("docs/../build"))
  }

  @Test
  fun `sanitizePath should allow relative paths with dots`() {
    assertEquals("./build", DocsPlugin.sanitizePath("./build"))
    assertEquals("docs/.vuepress/dist", DocsPlugin.sanitizePath("docs/.vuepress/dist"))
  }

  @Test
  fun `sanitizeBranch should remove dangerous characters`() {
    assertEquals("main", DocsPlugin.sanitizeBranch("main"))
    assertEquals("gh-pages", DocsPlugin.sanitizeBranch("gh-pages"))
    assertEquals("feature/docs", DocsPlugin.sanitizeBranch("feature/docs"))
    assertEquals("", DocsPlugin.sanitizeBranch("main;ls"))
  }

  @Test
  fun `sanitizeVersion should allow valid version strings`() {
    assertEquals("1.5.3", DocsPlugin.sanitizeVersion("1.5.3"))
    assertEquals("1.5.3-beta", DocsPlugin.sanitizeVersion("1.5.3-beta"))
    assertEquals("2.0.0_rc1", DocsPlugin.sanitizeVersion("2.0.0_rc1"))
  }

  @Test
  fun `sanitizeVersion should remove dangerous characters`() {
    assertEquals("1.5.3", DocsPlugin.sanitizeVersion("1.5.3;rm -rf /"))
    assertEquals("", DocsPlugin.sanitizeVersion("$(whoami)"))
  }

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
    assertFalse(DocsPlugin.isValidDomain("example-.com"))
    assertFalse(DocsPlugin.isValidDomain(".example.com"))
    assertFalse(DocsPlugin.isValidDomain("example.com-"))
    assertFalse(DocsPlugin.isValidDomain("example..com"))
    assertFalse(DocsPlugin.isValidDomain(""))
  }

  @Test
  fun `isValidDomain should reject domain with special characters`() {
    assertFalse(DocsPlugin.isValidDomain("example.com;ls"))
    assertFalse(DocsPlugin.isValidDomain("example.com/path"))
    assertFalse(DocsPlugin.isValidDomain("example.com:8080"))
  }
}
