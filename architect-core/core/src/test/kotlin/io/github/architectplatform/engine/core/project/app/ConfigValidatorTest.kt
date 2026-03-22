package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigValidatorTest {

  private val validator = ConfigValidator()

  // ── Required fields ────────────────────────────────────────────────

  @Test
  fun `valid config with project name passes`() {
    val config = mapOf("project" to mapOf("name" to "my-project"))
    val result = validator.validate(config)
    assertTrue(result.valid)
    assertTrue(result.errors.isEmpty())
  }

  @Test
  fun `missing project section produces error`() {
    val config = emptyMap<String, Any>()
    val result = validator.validate(config)
    assertFalse(result.valid)
    assertTrue(result.errors.any { it.contains("project.name") && it.contains("required") })
  }

  @Test
  fun `blank project name produces error`() {
    val config = mapOf("project" to mapOf("name" to ""))
    val result = validator.validate(config)
    assertFalse(result.valid)
    assertTrue(result.errors.any { it.contains("project.name") })
  }

  @Test
  fun `missing project name produces actionable hint`() {
    val config = mapOf("project" to mapOf("description" to "desc"))
    val result = validator.validate(config)
    assertFalse(result.valid)
    assertTrue(result.errors.any { it.contains("name:") })
  }

  @Test
  fun `missing project section suggests adding it`() {
    val config = mapOf("plugins" to emptyList<Any>())
    val result = validator.validate(config)
    assertFalse(result.valid)
    assertTrue(result.errors.any { it.contains("project:") && it.contains("name:") })
  }

  // ── Unknown keys ──────────────────────────────────────────────────

  @Test
  fun `unknown top-level key produces warning`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "unknown_key" to "value"
    )
    val result = validator.validate(config)
    assertTrue(result.valid)
    assertTrue(result.warnings.any { it.contains("unknown_key") })
  }

  @Test
  fun `unknown key warning suggests known keys`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "unknown_key" to "value"
    )
    val result = validator.validate(config)
    assertTrue(result.warnings.any { it.contains("did you mean") })
  }

  @Test
  fun `plugin context keys are recognized as known`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "gradle" to mapOf("enabled" to true)
    )
    val result = validator.validate(config, pluginContextKeys = setOf("gradle"))
    assertTrue(result.valid)
    assertTrue(result.warnings.isEmpty())
  }

  @Test
  fun `schema key is recognized`() {
    val config = mapOf(
      "\$schema" to "https://architect.dev/schema/architect.yml.json",
      "project" to mapOf("name" to "test")
    )
    // $schema triggers schema validation which may produce schema errors
    // but $schema itself should not produce an unknown-key warning
    val result = validator.validate(config)
    assertTrue(result.warnings.none { it.contains("\$schema") })
  }

  @Test
  fun `tasks key is recognized`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "tasks" to mapOf("build" to mapOf("run" to "echo build"))
    )
    val result = validator.validate(config)
    assertTrue(result.valid)
    assertTrue(result.warnings.isEmpty())
  }

  @Test
  fun `plugins key is recognized`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "plugins" to listOf(mapOf("name" to "git-architected"))
    )
    val result = validator.validate(config)
    assertTrue(result.valid)
    assertTrue(result.warnings.isEmpty())
  }

  // ── Line numbers ──────────────────────────────────────────────────

  @Test
  fun `line number is included in error when lineMap provided`() {
    val config = mapOf("project" to mapOf("description" to "no name"))
    val lineMap = mapOf("project" to 1, "project.description" to 2)
    val result = validator.validate(config, lineMap = lineMap)
    assertTrue(result.errors.any { it.startsWith("line 1:") })
  }

  @Test
  fun `line number is included in warning for unknown key`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "bogus" to "value"
    )
    val lineMap = mapOf("project" to 1, "bogus" to 3)
    val result = validator.validate(config, lineMap = lineMap)
    assertTrue(result.warnings.any { it.startsWith("line 3:") })
  }

  // ── Per-plugin schema validation ──────────────────────────────────

  @Test
  fun `plugin with configSchema validates its section`() {
    val plugin = TestPluginWithSchema()
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "testplugin" to mapOf("enabled" to "not-a-boolean") // should be boolean
    )
    val result = validator.validate(
      config,
      pluginContextKeys = setOf("testplugin"),
      plugins = listOf(plugin),
    )
    // Schema validation should produce an error about the wrong type
    assertTrue(result.errors.any { it.contains("[test-plugin]") && it.contains("testplugin") })
  }

  @Test
  fun `plugin without configSchema skips validation`() {
    val plugin = TestPluginNoSchema()
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "noplugin" to mapOf("anything" to "goes")
    )
    val result = validator.validate(
      config,
      pluginContextKeys = setOf("noplugin"),
      plugins = listOf(plugin),
    )
    assertTrue(result.valid)
    assertTrue(result.errors.isEmpty())
  }

  @Test
  fun `plugin with missing config section does not error`() {
    val plugin = TestPluginWithSchema()
    val config = mapOf("project" to mapOf("name" to "test"))
    val result = validator.validate(
      config,
      pluginContextKeys = setOf("testplugin"),
      plugins = listOf(plugin),
    )
    assertTrue(result.valid)
  }

  @Test
  fun `plugin with valid config section passes`() {
    val plugin = TestPluginWithSchema()
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "testplugin" to mapOf("enabled" to true)
    )
    val result = validator.validate(
      config,
      pluginContextKeys = setOf("testplugin"),
      plugins = listOf(plugin),
    )
    assertTrue(result.valid)
    assertTrue(result.errors.isEmpty())
  }

  // ── Multiple validations combined ─────────────────────────────────

  @Test
  fun `multiple errors and warnings are collected`() {
    val config = mapOf(
      "weird" to "value",
      "also_weird" to "value"
    )
    val result = validator.validate(config)
    assertFalse(result.valid)
    assertTrue(result.errors.size >= 1) // missing project.name
    assertEquals(2, result.warnings.size) // two unknown keys
  }

  // ── Test helpers ──────────────────────────────────────────────────

  private class TestPluginWithSchema : ArchitectPlugin<Any> {
    override val id = "test-plugin"
    override val contextKey = "testplugin"
    override val ctxClass: Class<Any> = Any::class.java
    override var context: Any = Any()
    override fun register(registry: TaskRegistry) {}

    override fun configSchema(): Map<String, Any> = mapOf(
      "type" to "object",
      "properties" to mapOf(
        "enabled" to mapOf("type" to "boolean")
      ),
      "additionalProperties" to false
    )
  }

  private class TestPluginNoSchema : ArchitectPlugin<Any> {
    override val id = "no-schema-plugin"
    override val contextKey = "noplugin"
    override val ctxClass: Class<Any> = Any::class.java
    override var context: Any = Any()
    override fun register(registry: TaskRegistry) {}
  }
}
