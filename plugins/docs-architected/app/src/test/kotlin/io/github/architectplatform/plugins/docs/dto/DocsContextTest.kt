package io.github.architectplatform.plugins.docs.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for DocsContext data class.
 */
class DocsContextTest {
  @Test
  fun `DocsContext should have default values`() {
    val context = DocsContext()

    assertNotNull(context.build)
    assertTrue(context.build.enabled)
    assertEquals("mkdocs", context.build.framework)
    assertEquals("docs", context.build.sourceDir)
    assertEquals("site", context.build.outputDir)

    assertNotNull(context.publish)
    assertTrue(context.publish.enabled)
    assertTrue(context.publish.githubPages)
    assertEquals("gh-pages", context.publish.branch)
  }

  @Test
  fun `DocsContext should accept custom values`() {
    val build = BuildContext(enabled = false, framework = "docusaurus", sourceDir = "documentation")
    val publish = PublishContext(enabled = false, githubPages = false, branch = "docs")
    val context = DocsContext(build = build, publish = publish)

    assertFalse(context.build.enabled)
    assertEquals("docusaurus", context.build.framework)
    assertEquals("documentation", context.build.sourceDir)

    assertFalse(context.publish.enabled)
    assertFalse(context.publish.githubPages)
    assertEquals("docs", context.publish.branch)
  }

  @Test
  fun `DocsContext equality should work correctly`() {
    val context1 = DocsContext()
    val context2 = DocsContext()

    assertEquals(context1, context2)
  }
}
