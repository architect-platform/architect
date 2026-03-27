package io.github.architectplatform.cli.command

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

class ConventionsCommandHandler {
  private val presetMapping = mapOf(
    "kotlin" to "kotlin-conventions",
    "typescript" to "typescript-conventions",
  )

  fun handle(args: List<String>) {
    val requestedPreset = args.getOrNull(1)?.trim()?.lowercase()
    val presetId = presetMapping[requestedPreset]
    if (presetId == null) {
      println("Usage: architect conventions <kotlin|typescript>")
      return
    }

    val configFile = File(System.getProperty("user.dir"), "architect.yml")
    if (!configFile.exists()) {
      println("❌ No architect.yml found in current directory")
      println("   Run 'architect init' to create one")
      return
    }

    val yaml = Yaml()
    @Suppress("UNCHECKED_CAST")
    val config = yaml.load<Map<String, Any>>(configFile.inputStream())?.toMutableMap() ?: mutableMapOf()

    ensureArchitecturePlugin(config)

    val architecture = (config["architecture"] as? Map<*, *>)?.toMutableMap()
      ?.mapKeys { it.key.toString() }?.toMutableMap()
      ?: mutableMapOf()
    val presetRulesets = ((architecture["presetRulesets"] as? List<*>) ?: emptyList<Any>())
      .map { it.toString() }
      .toMutableList()

    if (!presetRulesets.contains(presetId)) {
      presetRulesets += presetId
    }

    architecture["presetRulesets"] = presetRulesets.distinct()
    config["architecture"] = architecture

    val dumperOptions = DumperOptions().apply {
      defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
      isPrettyFlow = true
    }
    configFile.writeText(Yaml(dumperOptions).dump(config))

    println("✅ Applied convention preset '$requestedPreset' ($presetId)")
    println("   Run 'architect validate --structure' or 'architect architecture-validate' to verify compliance")
  }

  private fun ensureArchitecturePlugin(config: MutableMap<String, Any>) {
    val plugins = ((config["plugins"] as? List<*>) ?: emptyList<Any>())
      .mapNotNull { item ->
        when (item) {
          is Map<*, *> -> item.entries.associate { it.key.toString() to it.value }
          else -> null
        }
      }
      .toMutableList()

    if (plugins.none { it["name"] == "architecture-architected" }) {
      plugins += mapOf(
        "name" to "architecture-architected",
        "repo" to "architect-platform/architect",
      )
    }

    config["plugins"] = plugins
  }
}
