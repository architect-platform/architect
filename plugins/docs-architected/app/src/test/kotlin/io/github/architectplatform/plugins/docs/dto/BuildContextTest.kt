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
    assertEquals("1.5.3", context.mkdocsVersion)
    assertEquals("9.5.3", context.mkdocsMaterialVersion)
    assertEquals("My Project Documentation", context.siteName)
    assertEquals("Project documentation", context.siteDescription)
    assertEquals("Your Name", context.siteAuthor)
    assertEquals("", context.repoUrl)
    assertEquals("", context.repoName)
    assertEquals("indigo", context.primaryColor)
    assertEquals("indigo", context.accentColor)
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
            installDeps = false,
            mkdocsVersion = "1.6.0",
            mkdocsMaterialVersion = "9.6.0",
            siteName = "Custom Docs",
            siteDescription = "Custom description",
            siteAuthor = "John Doe",
            repoUrl = "https://github.com/user/repo",
            repoName = "user/repo",
            primaryColor = "blue",
            accentColor = "cyan")

    assertFalse(context.enabled)
    assertEquals("docusaurus", context.framework)
    assertEquals("documentation", context.sourceDir)
    assertEquals("build", context.outputDir)
    assertEquals("custom.yml", context.configFile)
    assertFalse(context.installDeps)
    assertEquals("1.6.0", context.mkdocsVersion)
    assertEquals("9.6.0", context.mkdocsMaterialVersion)
    assertEquals("Custom Docs", context.siteName)
    assertEquals("Custom description", context.siteDescription)
    assertEquals("John Doe", context.siteAuthor)
    assertEquals("https://github.com/user/repo", context.repoUrl)
    assertEquals("user/repo", context.repoName)
    assertEquals("blue", context.primaryColor)
    assertEquals("cyan", context.accentColor)
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

  @Test
  fun `BuildContext should allow version customization`() {
    val context = BuildContext(
        mkdocsVersion = "1.6.0",
        mkdocsMaterialVersion = "10.0.0"
    )

    assertEquals("1.6.0", context.mkdocsVersion)
    assertEquals("10.0.0", context.mkdocsMaterialVersion)
  }
}
