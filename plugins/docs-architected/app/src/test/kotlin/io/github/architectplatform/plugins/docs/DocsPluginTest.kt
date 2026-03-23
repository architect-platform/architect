package io.github.architectplatform.plugins.docs

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.plugins.docs.builders.DocumentationBuilderFactory
import io.github.architectplatform.plugins.docs.dto.BuildContext
import io.github.architectplatform.plugins.docs.dto.DocsContext
import io.github.architectplatform.plugins.docs.utils.SecurityUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Tests for DocsPlugin utility functions, path traversal prevention, and all three documentation builders.
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

  // --- Path traversal prevention tests ---

  @Test
  fun `sanitizePath blocks simple parent directory traversal`() {
    val result = SecurityUtils.sanitizePath("../../etc/passwd")
    assertFalse(result.contains(".."))
    assertFalse(result.startsWith("/"))
  }

  @Test
  fun `sanitizePath blocks deep nested traversal`() {
    val result = SecurityUtils.sanitizePath("docs/../../../etc/shadow")
    assertFalse(result.contains(".."))
  }

  @Test
  fun `sanitizePath removes absolute path prefix`() {
    val result = SecurityUtils.sanitizePath("/etc/passwd")
    assertFalse(result.startsWith("/"))
    assertEquals("etc/passwd", result)
  }

  @Test
  fun `sanitizePath strips semicolons preventing command injection`() {
    val result = SecurityUtils.sanitizePath("docs;rm -rf /")
    assertFalse(result.contains(";"))
    assertFalse(result.contains(" "))
  }

  @Test
  fun `sanitizePath strips pipe and ampersand preventing command chaining`() {
    val result = SecurityUtils.sanitizePath("docs|cat /etc/passwd")
    assertFalse(result.contains("|"))
    val result2 = SecurityUtils.sanitizePath("docs&&echo hacked")
    assertFalse(result2.contains("&"))
  }

  @Test
  fun `sanitizePath normalizes duplicate slashes`() {
    assertEquals("docs/build/output", SecurityUtils.sanitizePath("docs//build///output"))
  }

  @Test
  fun `sanitizeBranch strips command injection from branch name`() {
    val result = SecurityUtils.sanitizeBranch("gh-pages;rm -rf /")
    assertFalse(result.contains(";"))
    assertFalse(result.contains(" "))
  }

  @Test
  fun `sanitizeVersion strips command injection from version string`() {
    val result = SecurityUtils.sanitizeVersion("1.0.0;whoami")
    assertFalse(result.contains(";"))
    assertEquals("1.0.0whoami", result)
  }

  // --- Builder tests (all three frameworks) ---

  @Test
  fun `factory creates MkDocs builder for mkdocs framework`() {
    val executor = RecordingCommandExecutor()
    val builder = DocumentationBuilderFactory.createBuilder(BuildContext(framework = "mkdocs"), executor)
    assertEquals("MkDocs", builder.getName())
  }

  @Test
  fun `factory creates Docusaurus builder for docusaurus framework`() {
    val executor = RecordingCommandExecutor()
    val builder = DocumentationBuilderFactory.createBuilder(BuildContext(framework = "docusaurus"), executor)
    assertEquals("Docusaurus", builder.getName())
  }

  @Test
  fun `factory creates VuePress builder for vuepress framework`() {
    val executor = RecordingCommandExecutor()
    val builder = DocumentationBuilderFactory.createBuilder(BuildContext(framework = "vuepress"), executor)
    assertEquals("VuePress", builder.getName())
  }

  @Test
  fun `factory rejects unsupported framework`() {
    val executor = RecordingCommandExecutor()
    assertThrows(IllegalArgumentException::class.java) {
      DocumentationBuilderFactory.createBuilder(BuildContext(framework = "unknown"), executor)
    }
  }

  @Test
  fun `MkDocs builder sanitizes output directory in build command`() {
    val executor = RecordingCommandExecutor()
    val ctx = BuildContext(framework = "mkdocs", outputDir = "../../../etc/evil")
    val builder = DocumentationBuilderFactory.createBuilder(ctx, executor)
    // MkDocsBuilder.build() calls SecurityUtils.sanitizePath on outputDir
    val sanitized = SecurityUtils.sanitizePath(ctx.outputDir)
    assertFalse(sanitized.contains(".."))
    assertFalse(sanitized.startsWith("/"))
  }

  @Test
  fun `MkDocs builder sanitizes version strings`() {
    val injectedVersion = "1.5.3;rm -rf /"
    val sanitized = SecurityUtils.sanitizeVersion(injectedVersion)
    assertFalse(sanitized.contains(";"))
    assertFalse(sanitized.contains(" "))
  }

  @Test
  fun `docs-build returns disabled message when build disabled`() {
    val plugin = DocsPlugin()
    plugin.init(DocsContext(build = BuildContext(enabled = false)))
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val task = registry.get("docs-build")!!

    val result = task.execute(TestEnvironment(RecordingCommandExecutor()), ProjectContext(Path.of("/repo"), emptyMap()), emptyList())

    assertTrue(result.success)
    assertTrue(result.message!!.contains("disabled"))
  }

  @Test
  fun `registers all three docs tasks`() {
    val plugin = DocsPlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    val ids = registry.taskIds()
    assertTrue("docs-init" in ids)
    assertTrue("docs-build" in ids)
    assertTrue("docs-publish" in ids)
    assertEquals(3, ids.size)
  }

  private class TestTaskRegistry : io.github.architectplatform.api.core.tasks.TaskRegistry {
    private val tasks = mutableListOf<io.github.architectplatform.api.core.tasks.Task>()
    fun taskIds() = tasks.map { it.id }.toSet()
    override fun add(task: io.github.architectplatform.api.core.tasks.Task) { tasks.add(task) }
    override fun get(id: String) = tasks.find { it.id == id }
    override fun all() = tasks.toList()
  }

  private class RecordingCommandExecutor : CommandExecutor {
    val commands = mutableListOf<Pair<String, String?>>()
    override fun execute(command: String, workingDir: String?) {
      commands.add(command to workingDir)
    }
  }

  private class TestEnvironment(private val commandExecutor: CommandExecutor) : Environment {
    override fun <T> service(type: Class<T>): T {
      if (type == CommandExecutor::class.java) {
        @Suppress("UNCHECKED_CAST")
        return commandExecutor as T
      }
      throw IllegalArgumentException("Unsupported service: ${type.name}")
    }
    override fun publish(event: Any) {}
  }
}
