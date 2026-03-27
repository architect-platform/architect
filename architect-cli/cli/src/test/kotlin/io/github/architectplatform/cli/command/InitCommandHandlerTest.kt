package io.github.architectplatform.cli.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InitCommandHandlerTest {

  private val handler = InitCommandHandler()

  // ── Stack detection ─────────────────────────────────────────────

  @Test
  fun `detectStack finds gradle project`(@TempDir dir: File) {
    File(dir, "build.gradle.kts").writeText("plugins { }")
    val stack = handler.detectStack(dir)
    assertTrue(stack.languages.contains("Kotlin"))
    assertTrue(stack.buildTools.contains("Gradle"))
  }

  @Test
  fun `detectStack finds node project with package manager and test framework`(@TempDir dir: File) {
    File(dir, "package.json").writeText(
      """
        {
          "packageManager": "pnpm@9.0.0",
          "devDependencies": {
            "typescript": "^5.0.0",
            "vitest": "^2.0.0"
          }
        }
      """.trimIndent()
    )
    File(dir, "pnpm-lock.yaml").writeText("lockfileVersion: '9.0'")
    File(dir, "tsconfig.json").writeText("{}")
    val stack = handler.detectStack(dir)
    assertTrue(stack.languages.contains("TypeScript"))
    assertTrue(stack.buildTools.contains("pnpm"))
    assertTrue(stack.testFrameworks.contains("Vitest"))
  }

  @Test
  fun `detectStack finds python project`(@TempDir dir: File) {
    File(dir, "requirements.txt").writeText("flask")
    val stack = handler.detectStack(dir)
    assertTrue(stack.languages.contains("Python"))
    assertTrue(stack.buildTools.contains("pip"))
  }

  @Test
  fun `detectStack finds git repo`(@TempDir dir: File) {
    File(dir, ".git").mkdir()
    val stack = handler.detectStack(dir)
    assertTrue(".git" in stack.markers)
  }

  @Test
  fun `detectStack finds github workflows`(@TempDir dir: File) {
    File(dir, ".github/workflows").mkdirs()
    val stack = handler.detectStack(dir)
    assertTrue("GitHub Actions" in stack.ciSystems)
  }

  @Test
  fun `detectStack finds docs framework`(@TempDir dir: File) {
    File(dir, "mkdocs.yml").writeText("site_name: Test")
    val stack = handler.detectStack(dir)
    assertTrue("mkdocs.yml" in stack.markers)
  }

  @Test
  fun `detectStack returns empty for bare directory`(@TempDir dir: File) {
    val stack = handler.detectStack(dir)
    assertTrue(stack.languages.isEmpty())
    assertTrue(stack.buildTools.isEmpty())
  }

  @Test
  fun `detectStack finds multiple stacks`(@TempDir dir: File) {
    File(dir, "build.gradle.kts").writeText("")
    File(dir, "package.json").writeText("{}")
    File(dir, ".git").mkdir()
    val stack = handler.detectStack(dir)
    assertTrue(stack.languages.size >= 2)
    assertTrue(stack.buildTools.size >= 2)
  }

  // ── Plugin suggestions ──────────────────────────────────────────

  @Test
  fun `suggestPlugins suggests git plugin for git repo`(@TempDir dir: File) {
    File(dir, ".git").mkdir()
    val stack = handler.detectStack(dir)
    val suggestions = handler.suggestPlugins(stack)
    assertTrue(suggestions.any { it.id == "git-architected" })
  }

  @Test
  fun `suggestPlugins suggests gradle plugin for gradle project`(@TempDir dir: File) {
    File(dir, "build.gradle.kts").writeText("")
    val stack = handler.detectStack(dir)
    val suggestions = handler.suggestPlugins(stack)
    assertTrue(suggestions.any { it.id == "gradle-architected" })
  }

  @Test
  fun `suggestPlugins suggests docs plugin for mkdocs project`(@TempDir dir: File) {
    File(dir, "mkdocs.yml").writeText("")
    val stack = handler.detectStack(dir)
    val suggestions = handler.suggestPlugins(stack)
    assertTrue(suggestions.any { it.id == "docs-architected" })
  }

  @Test
  fun `suggestPlugins returns empty for bare directory`(@TempDir dir: File) {
    val stack = handler.detectStack(dir)
    val suggestions = handler.suggestPlugins(stack)
    assertTrue(suggestions.isEmpty())
  }

  // ── YAML generation ─────────────────────────────────────────────

  @Test
  fun `generateYaml creates minimal config`() {
    val yaml = handler.generateYaml("my-project", "", emptyList())
    assertTrue(yaml.contains("name: my-project"))
    assertTrue(!yaml.contains("description:"))
    assertTrue(!yaml.contains("plugins:"))
  }

  @Test
  fun `generateYaml includes description`() {
    val yaml = handler.generateYaml("my-project", "A cool project", emptyList())
    assertTrue(yaml.contains("description: \"A cool project\""))
  }

  @Test
  fun `generateYaml includes plugins`() {
    val plugins = listOf(
      InitCommandHandler.PluginSuggestion("git-architected", "architectplatform/git-architected", "Git detected"),
      InitCommandHandler.PluginSuggestion("gradle-architected", "architectplatform/gradle-architected", "Gradle detected"),
    )
    val yaml = handler.generateYaml("my-project", "", plugins)
    assertTrue(yaml.contains("plugins:"))
    assertTrue(yaml.contains("name: git-architected"))
    assertTrue(yaml.contains("repo: architectplatform/git-architected"))
    assertTrue(yaml.contains("name: gradle-architected"))
  }

  @Test
  fun `generateYaml includes header comment`() {
    val yaml = handler.generateYaml("test", "", emptyList())
    assertTrue(yaml.contains("# Architect project configuration"))
  }

  // ── Full integration ────────────────────────────────────────────

  @Test
  fun `full detection to yaml pipeline for kotlin gradle project`(@TempDir dir: File) {
    File(dir, "build.gradle.kts").writeText("")
    File(dir, ".git").mkdir()
    File(dir, ".github/workflows").mkdirs()
    File(dir, "mkdocs.yml").writeText("")

    val stack = handler.detectStack(dir)
    val plugins = handler.suggestPlugins(stack)
    val yaml = handler.generateYaml("test-project", "Test description", plugins)

    assertTrue(yaml.contains("name: test-project"))
    assertTrue(yaml.contains("description: \"Test description\""))
    assertTrue(yaml.contains("git-architected"))
    assertTrue(yaml.contains("github-architected"))
    assertTrue(yaml.contains("gradle-architected"))
    assertTrue(yaml.contains("docs-architected"))
    assertEquals(4, plugins.size)
  }
}
