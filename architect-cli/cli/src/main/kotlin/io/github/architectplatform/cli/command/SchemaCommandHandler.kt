package io.github.architectplatform.cli.command

import io.github.architectplatform.core.execution.EmbeddedExecutionContext
import io.github.architectplatform.core.schema.KotlinDataClassSchemaGenerator
import kotlin.system.exitProcess

/**
 * Handles the `architect schema` command.
 *
 * Subcommands:
 *   generate  — Loads all plugins for the current project and outputs a merged JSON Schema
 *               that combines the root architect.yml schema with each plugin's configSchema().
 *   show      — Outputs the bundled architect-schema.json (same as `architect schema` with no subcommand).
 */
class SchemaCommandHandler(
  private val activeProfile: String = "default",
) {

  fun handle(args: List<String>) {
    val subCommand = args.getOrNull(1) ?: "show"
    when (subCommand) {
      "generate" -> generateMergedSchema()
      "show", "print" -> showBundledSchema()
      "lint" -> lintConfigCommand()
      else -> {
        System.err.println("Unknown schema subcommand: $subCommand")
        System.err.println("Usage: architect schema [generate|show|lint]")
        exitProcess(1)
      }
    }
  }

  private fun showBundledSchema() {
    val stream = javaClass.classLoader.getResourceAsStream("architect-schema.json")
    if (stream != null) {
      print(stream.bufferedReader().readText())
      return
    }
    val file = java.io.File(System.getProperty("user.dir"), "docs/schema/architect-schema.json")
    if (file.exists()) {
      print(file.readText())
      return
    }
    System.err.println("Schema file not found.")
    exitProcess(1)
  }

  private fun generateMergedSchema() {
    val projectPath = System.getProperty("user.dir")
    val projectName = java.io.File(projectPath).name

    val context = EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)

    val rootSchema = loadRootSchema()
    val properties = mutableMapOf<String, Any>()
    (rootSchema["properties"] as? Map<*, *>)?.forEach { (key, value) ->
      if (key is String && value != null) {
        properties[key] = value
      }
    }

    if (project != null) {
      for (plugin in project.plugins) {
        val contextKey = plugin.contextKey
        val schema: Map<String, Any> = plugin.configSchema()
          ?: KotlinDataClassSchemaGenerator.generate(plugin.ctxClass)

        if (schema.isNotEmpty()) {
          properties[contextKey] = schema
        }
      }
    }

    val mergedSchema = rootSchema.toMutableMap()
    mergedSchema["properties"] = properties

    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
      .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
    println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mergedSchema))
  }

  private fun lintConfigCommand() {
    val projectPath = System.getProperty("user.dir")
    val configFile = java.io.File(projectPath, "architect.yml")
    if (!configFile.exists()) {
      System.err.println("❌ No architect.yml found in $projectPath")
      exitProcess(1)
    }

    val projectName = java.io.File(projectPath).name
    val context = EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)

    val issues = mutableListOf<String>()

    if (project == null) {
      issues += "Could not parse architect.yml"
    } else {
      val config = project.context.config
      val declaredPluginNames = (config["plugins"] as? List<*>)
        ?.filterIsInstance<Map<*, *>>()
        ?.mapNotNull { it["name"] as? String }
        ?: emptyList()

      val loadedPluginKeys = project.plugins.map { it.contextKey }.toSet()

      for (pluginName in declaredPluginNames) {
        val loaded = project.plugins.any { it.id == pluginName || it.contextKey == pluginName || it.id.contains(pluginName.removeSuffix("-architected")) }
        if (!loaded) {
          issues += "Plugin '$pluginName' is declared but could not be loaded"
        }
      }

      for (key in config.keys) {
        val knownKeys = setOf("project", "plugins", "affected") + loadedPluginKeys
        if (key !in knownKeys && !key.startsWith("_")) {
          issues += "Unknown config section '$key' — no matching plugin provides this context key"
        }
      }
    }

    if (issues.isEmpty()) {
      println("✅ architect.yml looks valid")
    } else {
      println("❌ architect.yml lint issues:")
      issues.forEach { println("  • $it") }
      exitProcess(1)
    }
  }

  private fun loadRootSchema(): MutableMap<String, Any> {
    val stream = javaClass.classLoader.getResourceAsStream("architect-schema.json")
    val json = stream?.bufferedReader()?.readText()
      ?: java.io.File(System.getProperty("user.dir"), "docs/schema/architect-schema.json")
        .takeIf { it.exists() }?.readText()
      ?: return mutableMapOf(
        "\$schema" to "http://json-schema.org/draft-07/schema#",
        "type" to "object",
        "properties" to mutableMapOf<String, Any>(),
      )
    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
    @Suppress("UNCHECKED_CAST")
    return mapper.readValue(json, MutableMap::class.java) as MutableMap<String, Any>
  }
}
