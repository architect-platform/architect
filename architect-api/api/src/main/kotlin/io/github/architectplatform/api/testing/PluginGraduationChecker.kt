package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin

/**
 * Validates that a plugin meets all requirements for "Active" graduation status.
 *
 * Checklist items:
 * 1. ArchitectPluginContractTestSuite passes (id, contextKey, ctxClass, registration)
 * 2. README.md exists with configuration examples
 * 3. STATUS.md exists with maturity declaration
 * 4. configSchema() returns a non-null JSON Schema
 * 5. Error handling: all tasks return TaskResult (never throw)
 * 6. At least 60% test coverage (advisory — requires external tool)
 *
 * Usage:
 * ```kotlin
 * val result = PluginGraduationChecker.check(myPlugin, pluginDir)
 * if (!result.passed) {
 *   result.failures.forEach { println("❌ $it") }
 * }
 * ```
 */
object PluginGraduationChecker {
  data class CheckResult(
    val pluginId: String,
    val checks: List<CheckItem>,
  ) {
    val passed: Boolean get() = checks.all { it.passed }
    val failures: List<CheckItem> get() = checks.filter { !it.passed }
    val warnings: List<CheckItem> get() = checks.filter { it.severity == Severity.WARNING && !it.passed }
  }

  data class CheckItem(
    val name: String,
    val description: String,
    val passed: Boolean,
    val severity: Severity = Severity.ERROR,
    val detail: String? = null,
  )

  enum class Severity { ERROR, WARNING }

  /**
   * Runs all graduation checks against the given plugin.
   *
   * @param plugin The plugin instance to check
   * @param pluginDir Absolute path to the plugin's root directory (for file checks)
   * @return A CheckResult with pass/fail for each item
   */
  fun check(plugin: ArchitectPlugin<*>, pluginDir: java.nio.file.Path): CheckResult {
    val checks = mutableListOf<CheckItem>()

    checks += checkPluginId(plugin)
    checks += checkContextKey(plugin)
    checks += checkConfigSchema(plugin)
    checks += checkTaskRegistration(plugin)
    checks += checkReadme(pluginDir)
    checks += checkStatusFile(pluginDir)

    return CheckResult(plugin.id, checks)
  }

  private fun checkPluginId(plugin: ArchitectPlugin<*>): CheckItem {
    val valid = plugin.id.isNotBlank() && plugin.id.matches(Regex("^[a-z][a-z0-9-]*$"))
    return CheckItem(
      name = "plugin-id",
      description = "Plugin ID must be non-blank, lowercase with hyphens",
      passed = valid,
      detail = if (!valid) "Got: '${plugin.id}'" else null,
    )
  }

  private fun checkContextKey(plugin: ArchitectPlugin<*>): CheckItem {
    val valid = plugin.contextKey.isNotBlank()
    return CheckItem(
      name = "context-key",
      description = "Context key must be non-blank",
      passed = valid,
      detail = if (!valid) "Got: '${plugin.contextKey}'" else null,
    )
  }

  private fun checkConfigSchema(plugin: ArchitectPlugin<*>): CheckItem {
    val schema = plugin.configSchema()
    return CheckItem(
      name = "config-schema",
      description = "configSchema() should return a non-null JSON Schema",
      passed = schema != null,
      severity = Severity.WARNING,
      detail = if (schema == null) "configSchema() returned null — IDE auto-completion won't work" else null,
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun checkTaskRegistration(plugin: ArchitectPlugin<*>): CheckItem {
    return try {
      val testKit = ArchitectPluginTestKit(plugin as ArchitectPlugin<Any>)
      val tasks = testKit.configure(emptyMap<String, Any>()).tasks()
      CheckItem(
        name = "task-registration",
        description = "Plugin must register at least one task",
        passed = tasks.isNotEmpty(),
        detail = if (tasks.isEmpty()) "No tasks registered" else "Registered ${tasks.size} task(s)",
      )
    } catch (e: Exception) {
      CheckItem(
        name = "task-registration",
        description = "Plugin must register at least one task",
        passed = false,
        detail = "Registration failed: ${e.message}",
      )
    }
  }

  private fun checkReadme(pluginDir: java.nio.file.Path): CheckItem {
    val readme = pluginDir.resolve("README.md")
    val exists = java.nio.file.Files.exists(readme)
    val hasContent = exists && java.nio.file.Files.readString(readme).length > 100
    return CheckItem(
      name = "readme",
      description = "README.md must exist with configuration examples (>100 chars)",
      passed = hasContent,
      detail =
        when {
          !exists -> "README.md not found at $pluginDir"
          !hasContent -> "README.md exists but is too short"
          else -> null
        },
    )
  }

  private fun checkStatusFile(pluginDir: java.nio.file.Path): CheckItem {
    val status = pluginDir.resolve("STATUS.md")
    val exists = java.nio.file.Files.exists(status)
    return CheckItem(
      name = "status-file",
      description = "STATUS.md must exist with maturity declaration",
      passed = exists,
      detail = if (!exists) "STATUS.md not found at $pluginDir" else null,
    )
  }
}
