package io.github.architectplatform.plugins.docs.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for BuildContext data class.
 */
class BuildContextTest {
  @Test
  fun `BuildContext should have default values`() {
    val context = BuildContext()

    assertTrue(context.enabled)
    assertEquals("mkdocs", context.framework)
    assertEquals("docs", context.sourceDir)
    assertEquals("site", context.outputDir)
    assertEquals("", context.configFile)
    assertTrue(context.installDeps)
  }

  @Test
  fun `BuildContext should accept custom values`() {
    val context =
        BuildContext(
            enabled = false,
            framework = "docusaurus",
            sourceDir = "documentation",
            outputDir = "build",
            configFile = "custom.yml",
            installDeps = false)

    assertFalse(context.enabled)
    assertEquals("docusaurus", context.framework)
    assertEquals("documentation", context.sourceDir)
    assertEquals("build", context.outputDir)
    assertEquals("custom.yml", context.configFile)
    assertFalse(context.installDeps)
  }

  @Test
  fun `BuildContext equality should work correctly`() {
    val context1 = BuildContext(framework = "vuepress")
    val context2 = BuildContext(framework = "vuepress")

    assertEquals(context1, context2)
  }

  @Test
  fun `BuildContext should support different frameworks`() {
    val mkdocs = BuildContext(framework = "mkdocs")
    val docusaurus = BuildContext(framework = "docusaurus")
    val vuepress = BuildContext(framework = "vuepress")
    val manual = BuildContext(framework = "manual")

    assertEquals("mkdocs", mkdocs.framework)
    assertEquals("docusaurus", docusaurus.framework)
    assertEquals("vuepress", vuepress.framework)
    assertEquals("manual", manual.framework)
  }
}
