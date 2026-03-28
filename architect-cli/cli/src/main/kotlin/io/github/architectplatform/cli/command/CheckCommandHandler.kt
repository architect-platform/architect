package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.core.env.EnvFileLoader
import io.github.architectplatform.core.tasks.application.ConditionCheckResult
import io.github.architectplatform.core.tasks.application.ConditionIssue
import io.github.architectplatform.core.tasks.application.IssueKind
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Handles the `architect check` command: runs task precondition checks without executing.
 */
class CheckCommandHandler(
  private val embeddedTaskExecutor: EmbeddedTaskExecutor,
  private val extractProjectName: (String) -> String,
  private val exit: (Int) -> Unit = { code -> exitProcess(code) },
  private val envProvider: () -> Map<String, String> = { System.getenv() },
) {

  var json: Boolean = false

  fun handle(args: List<String>) {
    val taskFilter = args.getOrNull(1)
    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    val context = io.github.architectplatform.core.execution.EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: throw IllegalArgumentException("Project $projectName is not registered")
    val allTasks = context.getAllTasks(projectName)
    val checker = io.github.architectplatform.core.tasks.application.TaskConditionChecker()

    val tasksToCheck = if (taskFilter != null) {
      allTasks.filter { it.id == taskFilter }.also {
        if (it.isEmpty()) {
          println("No task found with id '$taskFilter'")
          exit(1)
        }
      }
    } else {
      allTasks
    }

    val results = checker.checkAll(tasksToCheck).toMutableList()
    val discovered = discoverEnvironmentVariables(project)
    updateEnvExample(Path.of(project.path), discovered.discovered)
    val envResult = buildEnvironmentCheckResult(
      requiredVars = discovered.required,
      projectDir = Path.of(project.path),
      profile = embeddedTaskExecutor.activeProfile,
    )
    if (envResult != null) {
      results += envResult
    }

    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results))
      if (results.any { !it.satisfied }) {
        exit(1)
      }
      return
    }

    val ok = results.filter { it.satisfied }
    val blocked = results.filter { !it.satisfied }

    println()
    println("━".repeat(80))
    println("🔍 Task Precondition Check — $projectName")
    println("━".repeat(80))
    println()

    if (blocked.isNotEmpty()) {
      println("  ❌ BLOCKED (${blocked.size})")
      println()
      blocked.forEach { result ->
        println("  ╔══ ${result.taskId}")
        result.issues.forEach { issue ->
          println("  ║  ⚠  ${issue.message}")
          println("  ║     → ${issue.hint}")
        }
        println("  ╚══")
        println()
      }
    }

    if (ok.isNotEmpty()) {
      println("  ✅ READY (${ok.size})")
      ok.chunked(4).forEach { chunk ->
        println("    " + chunk.joinToString("  ") { it.taskId })
      }
      println()
    }

    println("  ${ok.size} ready, ${blocked.size} blocked")
    println()

    if (blocked.isNotEmpty()) exit(1)
  }

  private data class DiscoveredEnvironmentVariables(
    val required: Set<String>,
    val discovered: Set<String>,
  )

  private data class ConfigReference(
    val name: String,
    val hasDefault: Boolean,
  )

  private fun discoverEnvironmentVariables(
    project: io.github.architectplatform.core.project.domain.Project,
  ): DiscoveredEnvironmentVariables {
    val requiredFromPluginMetadata = project.plugins
      .flatMap { plugin -> plugin.requiredEnvironmentVariables() }
      .toMutableSet()
    val requiredFromSchemas = mutableSetOf<String>()
    project.plugins.forEach { plugin ->
      val schema = plugin.configSchema()
      if (schema != null) {
        requiredFromSchemas += readTopLevelRequiredEnv(schema)
        requiredFromSchemas += readNestedRequiredEnv(schema)
      }
    }

    val configRefs = readConfigEnvReferences(
      projectDir = Path.of(project.path),
      profile = embeddedTaskExecutor.activeProfile,
    )
    val requiredFromConfig = configRefs.filter { !it.hasDefault }.map { it.name }.toSet()
    val discoveredFromConfig = configRefs.map { it.name }.toSet()

    val required = (requiredFromPluginMetadata + requiredFromSchemas + requiredFromConfig)
      .toSortedSet()
    val discovered = (required + discoveredFromConfig).toSortedSet()
    return DiscoveredEnvironmentVariables(required = required, discovered = discovered)
  }

  private fun readTopLevelRequiredEnv(schema: Map<String, Any>): Set<String> =
    (schema["requiredEnv"] as? List<*>)?.mapNotNull { it as? String }?.toSet().orEmpty()

  private fun readNestedRequiredEnv(node: Any?): Set<String> {
    if (node == null) return emptySet()
    return when (node) {
      is Map<*, *> -> {
        val current = (node["x-required-env"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val nested = node.values.flatMap { value -> readNestedRequiredEnv(value) }
        (current + nested).toSet()
      }

      is List<*> -> node.flatMap { value -> readNestedRequiredEnv(value) }.toSet()
      else -> emptySet()
    }
  }

  private fun readConfigEnvReferences(projectDir: Path, profile: String): List<ConfigReference> {
    val placeholderRegex = Regex("""\$\{env\.([A-Za-z_][A-Za-z0-9_]*)(?::[^}]*)?}""")
    val defaultRegex = Regex("""\$\{env\.([A-Za-z_][A-Za-z0-9_]*):[^}]*}""")
    return readRawConfigFiles(projectDir, profile)
      .flatMap { rawConfig ->
        val withDefaults = defaultRegex.findAll(rawConfig).map { it.groupValues[1] }.toSet()
        placeholderRegex.findAll(rawConfig).map { match ->
          val varName = match.groupValues[1]
          ConfigReference(name = varName, hasDefault = varName in withDefaults)
        }
      }
      .distinctBy { it.name to it.hasDefault }
  }

  private fun buildEnvironmentCheckResult(
    requiredVars: Set<String>,
    projectDir: Path,
    profile: String,
  ): ConditionCheckResult? {
    if (requiredVars.isEmpty()) return null
    val effectiveEnv = EnvFileLoader.load(projectDir, profile, emptyMap())
      .toMutableMap()
      .apply { putAll(envProvider()) }
    val missing = requiredVars.filter { name -> effectiveEnv[name].isNullOrBlank() }.sorted()
    if (missing.isEmpty()) return null

    val issues = missing.map { envVar ->
      ConditionIssue(
        kind = IssueKind.ENV,
        message = "Required environment variable '$envVar' is not set",
        hint = buildEnvHint(envVar, profile),
      )
    }

    return ConditionCheckResult(
      taskId = "environment",
      satisfied = false,
      issues = issues,
    )
  }

  private fun buildEnvHint(envVar: String, profile: String): String {
    val profileHint = if (profile == "default") {
      "Add it to .env"
    } else {
      "Add it to .env.$profile or .env.$profile.local (or .env)"
    }
    return "export $envVar=<value>  •  $profileHint"
  }

  private fun readRawConfigFiles(projectDir: Path, profile: String): List<String> {
    val rawConfigs = mutableListOf<String>()
    projectDir.resolve("architect.yml").toFile().takeIf { it.exists() }?.readText()?.let(rawConfigs::add)
      ?: projectDir.resolve("architect.yaml").toFile().takeIf { it.exists() }
        ?.readText()
        ?.let(rawConfigs::add)

    val contextDir = projectDir.resolve(".architect").toFile()
    if (contextDir.exists() && contextDir.isDirectory) {
      contextDir.listFiles { file ->
        file.isFile && (file.extension.equals("yml", ignoreCase = true) || file.extension.equals("yaml", ignoreCase = true))
      }
        ?.sortedBy { it.name }
        ?.forEach { rawConfigs += it.readText() }
    }

    if (profile != "default") {
      projectDir.resolve("architect.$profile.yml").toFile().takeIf { it.exists() }?.readText()?.let(rawConfigs::add)
        ?: projectDir.resolve("architect.$profile.yaml").toFile().takeIf { it.exists() }
          ?.readText()
          ?.let(rawConfigs::add)
    }

    return rawConfigs
  }

  private fun updateEnvExample(projectDir: Path, discoveredVars: Set<String>) {
    if (discoveredVars.isEmpty()) return
    val envExample = projectDir.resolve(".env.example").toFile()
    val sorted = discoveredVars.toSortedSet()

    if (!envExample.exists()) {
      envExample.writeText(sorted.joinToString(separator = "\n", postfix = "\n") { "$it=" })
      return
    }

    val existingLines = envExample.readLines()
    val existingKeys = EnvFileLoader.parseEnvFile(envExample.readText()).keys
    val missingKeys = sorted.filterNot { it in existingKeys }
    if (missingKeys.isEmpty()) return

    val separator = if (existingLines.isEmpty() || existingLines.last().isBlank()) "" else "\n"
    val appendBlock = missingKeys.joinToString(separator = "\n", prefix = separator, postfix = "\n") { "$it=" }
    envExample.appendText(appendBlock)
  }
}
