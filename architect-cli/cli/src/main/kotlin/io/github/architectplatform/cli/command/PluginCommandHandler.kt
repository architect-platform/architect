package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.plugin.PluginDocumentationGenerator
import io.github.architectplatform.cli.plugin.PluginJarValidator
import io.github.architectplatform.cli.plugin.PluginScaffolder
import io.github.architectplatform.cli.plugin.PluginTemplate
import io.github.architectplatform.api.testing.PluginGraduationChecker
import io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher
import io.github.architectplatform.core.plugin.app.IsolatedPluginClassLoader
import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.core.plugin.app.SpiPluginLoader
import io.github.architectplatform.core.plugin.infra.PluginRegistry
import io.github.architectplatform.core.plugin.infra.PluginRegistryEntry
import java.nio.file.Path
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import kotlin.system.exitProcess

/**
 * Handles plugin subcommands: validate, docs, create, search, install.
 */
class PluginCommandHandler(
  private val pluginScaffolder: PluginScaffolder = PluginScaffolder(),
  private val pluginDocumentationGenerator: PluginDocumentationGenerator = PluginDocumentationGenerator(),
  private val pluginJarValidator: PluginJarValidator = PluginJarValidator(),
  private val remoteContentFetcher: RemoteContentFetcher = JdkRemoteContentFetcher(),
  private val registryUrl: String = DEFAULT_REGISTRY_URL,
) {

  var json: Boolean = false

  fun handle(args: List<String>) {
    val subCommand = args.getOrNull(1)
    when (subCommand) {
      "validate" -> handleValidate(args)
      "docs" -> handleDocs(args)
      "create" -> handleCreate(args)
      "search" -> handleSearch(args)
      "install" -> handleInstall(args)
      "outdated" -> handleOutdated()
      "update" -> handleUpdate(args)
      "graduate" -> handleGraduate(args)
      "test" -> handleTest(args)
      else -> {
        println("Usage: architect plugin <create|docs|validate|search|install|outdated|update|graduate|test> [args]")
        println()
        println("Commands:")
        println("  docs <path>              Generate PLUGIN_REFERENCE.md from plugin metadata")
        println("  validate <path>          Validate a plugin JAR and its SPI/config wiring")
        println("  create <name> [template]  Scaffold a new plugin (kotlin, typescript, go)")
        println("  search <query>       Search the plugin registry")
        println("  install <plugin-id>  Add a plugin to architect.yml")
        println("  outdated             List plugins with available updates")
        println("  update [<id>|--all]  Update plugin version pins to latest")
        println("  graduate <jar-path>  Check if plugin meets graduation checklist")
        println("  test <jar-path>      Run plugin contract checks against a plugin JAR")
        exitProcess(1)
      }
    }
  }

  private fun handleValidate(args: List<String>) {
    val pluginPath = args.getOrNull(2)
    if (pluginPath == null) {
      println("Usage: architect plugin validate <path>")
      exitProcess(1)
    }

    try {
      val validation = pluginJarValidator.validate(java.nio.file.Path.of(pluginPath))
      if (json) {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(validation))
      } else {
        val statusIcon = if (validation.valid) "✅" else "❌"
        println("$statusIcon Plugin validation ${if (validation.valid) "passed" else "failed"} for ${validation.jarPath}")
        println("SPI providers: ${validation.spiImplementations.size}")
        validation.plugins.forEach { plugin ->
          println("- ${plugin.pluginId} (${plugin.className})")
          println("  contextKey=${plugin.contextKey}, ctxClass=${plugin.contextClass}, config=${if (plugin.configDeserializationValid) "ok" else "failed"}")
        }
        if (validation.warnings.isNotEmpty()) {
          println("Warnings:")
          validation.warnings.forEach { println("- $it") }
        }
        if (validation.errors.isNotEmpty()) {
          println("Errors:")
          validation.errors.forEach { println("- $it") }
        }
      }
      if (!validation.valid) {
        exitProcess(1)
      }
    } catch (e: Exception) {
      println("Failed to validate plugin JAR: ${e.message}")
      exitProcess(1)
    }
  }

  private fun handleDocs(args: List<String>) {
    val pluginPath = args.getOrNull(2)
    if (pluginPath == null) {
      println("Usage: architect plugin docs <path>")
      exitProcess(1)
    }

    try {
      val generatedDocs = pluginDocumentationGenerator.generate(java.nio.file.Path.of(pluginPath))
      if (json) {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        println(
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
            mapOf(
              "plugin" to generatedDocs.manifest.name,
              "template" to generatedDocs.manifest.template,
              "outputPath" to generatedDocs.outputPath.toString(),
              "tasks" to generatedDocs.tasks.map { it.id },
            ),
          ),
        )
      } else {
        println("✅ Generated plugin reference doc at ${generatedDocs.outputPath}")
      }
    } catch (e: Exception) {
      println("Failed to generate plugin docs: ${e.message}")
      exitProcess(1)
    }
  }

  private fun handleCreate(args: List<String>) {
    val pluginName = args.getOrNull(2)
    if (pluginName == null) {
      println("Usage: architect plugin create <name> [template|--template <template>]")
      exitProcess(1)
    }

    val template = resolvePluginTemplate(args)
    if (template == null) {
      println("Unknown template. Supported templates: kotlin, typescript, go")
      exitProcess(1)
    }

    try {
      val scaffoldPath = pluginScaffolder.scaffold(
        name = pluginName,
        template = template,
        targetRoot = java.nio.file.Path.of(System.getProperty("user.dir")),
      )
      println("✅ Created ${template.id} plugin scaffold at $scaffoldPath")
    } catch (e: Exception) {
      println("Failed to create plugin scaffold: ${e.message}")
      exitProcess(1)
    }
  }

  private fun handleSearch(args: List<String>) {
    val query = args.getOrNull(2)
    if (query == null) {
      println("Usage: architect plugin search <query>")
      exitProcess(1)
    }
    try {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      val registryJson = remoteContentFetcher.fetchText(registryUrl)
      val registry = mapper.readValue(registryJson, io.github.architectplatform.core.plugin.infra.PluginRegistry::class.java)
      val lowerQuery = query.lowercase()
      val results = registry.plugins.filter {
        it.id.lowercase().contains(lowerQuery) ||
          (it.description?.lowercase()?.contains(lowerQuery) == true)
      }
      if (results.isEmpty()) {
        println("No plugins found matching '$query'")
        return
      }
      if (json) {
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results))
      } else {
        println()
        println("━".repeat(60))
        println("🔍 Plugin Search: $query")
        println("━".repeat(60))
        val fmt = "  %-30s  %-10s  %s"
        println(fmt.format("ID", "VERSION", "DESCRIPTION"))
        println("  ${"─".repeat(56)}")
        results.forEach { p ->
          println(fmt.format(p.id.take(30), p.version.take(10), (p.description ?: "").take(20)))
        }
        println()
      }
    } catch (e: Exception) {
      println("Failed to search registry: ${e.message}")
      exitProcess(1)
    }
  }

  private fun handleInstall(args: List<String>) {
    val pluginId = args.getOrNull(2)
    if (pluginId == null) {
      println("Usage: architect plugin install <plugin-id>")
      exitProcess(1)
    }
    val projectPath = System.getProperty("user.dir")
    val configFile = java.io.File(projectPath, "architect.yml")
    if (!configFile.exists()) {
      println("No architect.yml found in $projectPath")
      exitProcess(1)
    }
    val content = configFile.readText()
    val pluginEntry = "\n  - name: $pluginId"
    if (content.contains("plugins:")) {
      configFile.writeText(content.replaceFirst("plugins:", "plugins:$pluginEntry"))
    } else {
      configFile.appendText("\nplugins:$pluginEntry\n")
    }
    println("✅ Added plugin '$pluginId' to architect.yml")
  }

  fun maybeWarnOutdatedPlugins(projectPath: String = System.getProperty("user.dir")) {
    runCatching {
      val outdated = findOutdatedPlugins(projectPath)
      if (outdated.isNotEmpty()) {
        println("ℹ️  ${outdated.size} plugin update(s) available. Run: architect plugin outdated")
      }
    }
  }

  private fun handleOutdated() {
    val outdated = findOutdatedPlugins(System.getProperty("user.dir"))
    if (outdated.isEmpty()) {
      println("✅ All configured plugins are up to date")
      return
    }
    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outdated))
      return
    }
    println()
    println("━".repeat(80))
    println("⬆️  Plugin Updates Available")
    println("━".repeat(80))
    val fmt = "  %-28s  %-12s  %-12s"
    println(fmt.format("PLUGIN", "CURRENT", "LATEST"))
    println("  ${"─".repeat(56)}")
    outdated.forEach { info ->
      println(fmt.format(info.id.take(28), info.currentVersion.take(12), info.latestVersion.take(12)))
    }
    println()
  }

  private fun handleUpdate(args: List<String>) {
    val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
    if (!configFile.exists()) {
      println("No architect.yml found in ${configFile.parent}")
      exitProcess(1)
    }

    val updateAll = args.contains("--all")
    val targetId = args.getOrNull(2)?.takeIf { !it.startsWith("--") }
    if (!updateAll && targetId == null) {
      println("Usage: architect plugin update <plugin-id> | --all")
      exitProcess(1)
    }

    val yaml = Yaml()
    val config = (yaml.load<Map<String, Any>>(configFile.inputStream()) ?: emptyMap()).toMutableMap()
    val pluginEntries = mutablePluginEntries(config)
    val outdated = findOutdatedPlugins(System.getProperty("user.dir"))
    val targetOutdated = if (updateAll) outdated else outdated.filter { it.id == targetId }

    if (targetOutdated.isEmpty()) {
      println("✅ No plugin updates to apply")
      return
    }

    var updatedCount = 0
    targetOutdated.forEach { info ->
      val entry = pluginEntries.firstOrNull { (it["name"] as? String) == info.id } ?: return@forEach
      entry["version"] = info.latestVersion
      updatedCount++
    }

    val dumpOptions = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
    configFile.writeText(Yaml(dumpOptions).dump(config))
    println("✅ Updated $updatedCount plugin(s) in architect.yml")
  }

  private fun findOutdatedPlugins(projectPath: String): List<PluginUpdateInfo> {
    val configFile = java.io.File(projectPath, "architect.yml")
    if (!configFile.exists()) return emptyList()

    val yaml = Yaml()
    val config = yaml.load<Map<String, Any>>(configFile.inputStream()) ?: emptyMap()
    val pluginEntries = mutablePluginEntries(config)
    if (pluginEntries.isEmpty()) return emptyList()

    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
      .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
    val registryJson = remoteContentFetcher.fetchText(registryUrl)
    val registry = mapper.readValue(registryJson, PluginRegistry::class.java)
    val registryById = registry.plugins.associateBy(PluginRegistryEntry::id)

    return pluginEntries.mapNotNull { entry ->
      val id = entry["name"] as? String ?: return@mapNotNull null
      val current = (entry["version"] as? String)?.trim().orEmpty().ifBlank { "latest" }
      if (current == "latest") return@mapNotNull null
      val latest = registryById[id]?.version ?: return@mapNotNull null
      if (compareVersions(current, latest) < 0) PluginUpdateInfo(id, current, latest) else null
    }
  }

  private fun mutablePluginEntries(config: Map<String, Any>): MutableList<MutableMap<String, Any>> {
    val raw = config["plugins"] as? List<*> ?: return mutableListOf()
    return raw.mapNotNull { entry ->
      @Suppress("UNCHECKED_CAST")
      entry as? MutableMap<String, Any> ?: (entry as? Map<String, Any>)?.toMutableMap()
    }.toMutableList()
  }

  private fun compareVersions(a: String, b: String): Int {
    val partsA = a.removePrefix("v").split(".")
    val partsB = b.removePrefix("v").split(".")
    for (i in 0 until maxOf(partsA.size, partsB.size)) {
      val nA = partsA.getOrNull(i)?.toIntOrNull() ?: 0
      val nB = partsB.getOrNull(i)?.toIntOrNull() ?: 0
      if (nA != nB) return nA - nB
    }
    return 0
  }

  private fun resolvePluginTemplate(arguments: List<String>): PluginTemplate? {
    var positionalTemplate: String? = null
    var index = 3
    while (index < arguments.size) {
      val argument = arguments[index]
      when {
        argument == "--template" -> {
          return arguments.getOrNull(index + 1)?.let(PluginTemplate::from)
        }
        argument.startsWith("--template=") -> {
          return PluginTemplate.from(argument.substringAfter('='))
        }
        !argument.startsWith("--") && positionalTemplate == null -> {
          positionalTemplate = argument
        }
      }
      index += 1
    }

    return PluginTemplate.from(positionalTemplate ?: "kotlin")
  }

  companion object {
    private const val DEFAULT_REGISTRY_URL = "https://registry.architect.dev/registry.json"
  }

  data class PluginUpdateInfo(
    val id: String,
    val currentVersion: String,
    val latestVersion: String,
  )

  private fun handleGraduate(args: List<String>) {
    val jarPath = args.getOrNull(2)
    if (jarPath == null) {
      println("Usage: architect plugin graduate <jar-path> [--plugin-dir <dir>]")
      exitProcess(1)
    }

    val pluginDir = args.indexOf("--plugin-dir").let { idx ->
      if (idx >= 0) args.getOrNull(idx + 1) else null
    } ?: Path.of(jarPath).parent?.toString() ?: "."

    println("🎓 Running graduation checklist for: $jarPath")
    println()

    // Load plugins from JAR
    val classLoader = IsolatedPluginClassLoader(
      urls = arrayOf(Path.of(jarPath).toUri().toURL()),
      parent = javaClass.classLoader,
    )
    val plugins = classLoader.use { SpiPluginLoader().loadFrom(it) }

    if (plugins.isEmpty()) {
      println("❌ No plugins found in JAR")
      exitProcess(1)
    }

    var allPassed = true
    for (plugin in plugins) {
      val result = PluginGraduationChecker.check(plugin, Path.of(pluginDir))
      println("Plugin: ${result.pluginId}")
      println("─".repeat(40))

      for (check in result.checks) {
        val icon = if (check.passed) "✅" else if (check.severity == PluginGraduationChecker.Severity.WARNING) "⚠️" else "❌"
        println("  $icon ${check.name}: ${check.description}")
        check.detail?.let { println("     └─ $it") }
      }

      println()
      if (result.passed) {
        println("✅ Plugin '${result.pluginId}' meets graduation requirements!")
      } else {
        val errorCount = result.failures.count { it.severity == PluginGraduationChecker.Severity.ERROR }
        val warnCount = result.warnings.size
        println("❌ Plugin '${result.pluginId}' has $errorCount error(s) and $warnCount warning(s)")
        allPassed = false
      }
      println()
    }

    if (!allPassed) exitProcess(1)
  }

  private fun handleTest(args: List<String>) {
    val jarPath = args.getOrNull(2)
    if (jarPath == null) {
      println("Usage: architect plugin test <jar-path>")
      exitProcess(1)
    }

    try {
      val result = pluginJarValidator.test(Path.of(jarPath))
      if (json) {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result))
      } else {
        val status = if (result.valid) "✅" else "❌"
        println("$status Plugin test ${if (result.valid) "passed" else "failed"} for ${result.jarPath}")
        println("SPI providers: ${result.spiImplementations.size}")
        result.plugins.forEach { plugin ->
          println("- ${plugin.pluginId} (${plugin.className})")
          plugin.checks.forEach { check ->
            val icon = if (check.passed) "✅" else "❌"
            println("  $icon ${check.name}: ${check.detail}")
          }
        }
        if (result.warnings.isNotEmpty()) {
          println("Warnings:")
          result.warnings.forEach { println("- $it") }
        }
        if (result.errors.isNotEmpty()) {
          println("Errors:")
          result.errors.forEach { println("- $it") }
        }
      }
      if (!result.valid) {
        exitProcess(1)
      }
    } catch (e: Exception) {
      println("Failed to test plugin JAR: ${e.message}")
      exitProcess(1)
    }
  }
}
