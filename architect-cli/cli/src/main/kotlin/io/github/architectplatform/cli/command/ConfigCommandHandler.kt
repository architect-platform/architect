package io.github.architectplatform.cli.command

import kotlin.math.min
import kotlin.system.exitProcess

/**
 * Handles `architect config` — configuration management subcommands.
 *
 * Subcommands:
 * - `architect config show`     — display current resolved configuration
 * - `architect config validate` — validate config against schema
 * - `architect config lint`     — lint config for deprecated keys and mistakes
 * - `architect config get key`  — get a config value by dotted key
 * - `architect config set key value` — set a config value by dotted key
 */
class ConfigCommandHandler(
    private val exit: (Int) -> Unit = { code -> exitProcess(code) },
) {

    var json: Boolean = false
    var plain: Boolean = false

    fun handle(args: List<String>) {
        val subcommand = args.getOrNull(1) ?: "show"
        when (subcommand) {
            "show" -> handleShow()
            "validate" -> handleValidate()
            "lint" -> handleLint()
            "get" -> handleGet(args.getOrNull(2))
            "set" -> handleSet(args.getOrNull(2), args.getOrNull(3))
            else -> {
                println("Unknown config subcommand: $subcommand")
                println("Available: show, validate, lint, get, set")
            }
        }
    }

    private fun handleShow() {
        val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
        if (!configFile.exists()) {
            println("❌ No architect.yml found in current directory")
            println("   Run 'architect init' to create one")
            return
        }
        println()
        println("📋 Current Configuration")
        println("━".repeat(60))
        println("   File: ${configFile.absolutePath}")
        println()

        val yaml = org.yaml.snakeyaml.Yaml()
        @Suppress("UNCHECKED_CAST")
        val config = yaml.load<Map<String, Any>>(configFile.inputStream())
        if (config == null) {
            println("   (empty configuration)")
            return
        }
        printConfigMap(config, indent = 3)
        println()
    }

    private fun handleValidate() {
        val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
        if (!configFile.exists()) {
            println("❌ No architect.yml found")
            return
        }
        val errors = mutableListOf<String>()
        try {
            val yaml = org.yaml.snakeyaml.Yaml()
            @Suppress("UNCHECKED_CAST")
            val config = yaml.load<Map<String, Any>>(configFile.inputStream())
            if (config == null) {
                errors.add("Configuration file is empty")
            } else {
                // Check required fields
                @Suppress("UNCHECKED_CAST")
                val project = config["project"] as? Map<String, Any>
                if (project == null) errors.add("Missing 'project' section")
                else if (project["name"] == null) errors.add("Missing 'project.name'")

                // Check plugins format
                val plugins = config["plugins"]
                if (plugins != null && plugins !is List<*>) {
                    errors.add("'plugins' should be a list")
                }
            }
        } catch (e: Exception) {
            errors.add("YAML parse error: ${e.message}")
        }

        if (errors.isEmpty()) {
            println("✅ Configuration is valid")
        } else {
            println("❌ Configuration has ${errors.size} issue(s):")
            errors.forEach { println("   • $it") }
        }
    }

    private fun handleLint() {
        val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
        if (!configFile.exists()) {
            println("❌ No architect.yml found")
            exit(1)
            return
        }

        val issues = mutableListOf<String>()
        try {
            val yaml = org.yaml.snakeyaml.Yaml()
            @Suppress("UNCHECKED_CAST")
            val config = yaml.load<Map<String, Any>>(configFile.inputStream())
            if (config == null) {
                issues.add("Configuration file is empty")
            } else {
                collectValidationIssues(config, issues)
                collectDeprecatedKeyIssues(config, issues)
                collectTopLevelKeySuggestionIssues(config, issues)
                collectPluginIssues(config, issues)
            }
        } catch (e: Exception) {
            issues.add("YAML parse error: ${e.message}")
        }

        if (issues.isEmpty()) {
            println("✅ Configuration lint passed")
            return
        }

        println("❌ Configuration lint found ${issues.size} issue(s):")
        issues.forEach { println("   • $it") }
        exit(1)
    }

    private fun handleGet(key: String?) {
        if (key == null) {
            println("Usage: architect config get <key>")
            println("Example: architect config get project.name")
            return
        }
        val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
        if (!configFile.exists()) {
            println("❌ No architect.yml found")
            return
        }
        val yaml = org.yaml.snakeyaml.Yaml()
        @Suppress("UNCHECKED_CAST")
        val config = yaml.load<Map<String, Any>>(configFile.inputStream()) ?: emptyMap()

        val value = getNestedValue(config, key.split("."))
        if (value != null) {
            println(value)
        } else {
            println("Key '$key' not found")
        }
    }

    private fun handleSet(key: String?, value: String?) {
        if (key == null || value == null) {
            println("Usage: architect config set <key> <value>")
            println("Example: architect config set project.name my-project")
            return
        }
        val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
        if (!configFile.exists()) {
            println("❌ No architect.yml found")
            return
        }
        val yaml = org.yaml.snakeyaml.Yaml()
        @Suppress("UNCHECKED_CAST")
        val config = yaml.load<Map<String, Any>>(configFile.inputStream())?.toMutableMap() ?: mutableMapOf()

        setNestedValue(config, key.split("."), value)

        val dumperOptions = org.yaml.snakeyaml.DumperOptions().apply {
            defaultFlowStyle = org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        val output = org.yaml.snakeyaml.Yaml(dumperOptions).dump(config)
        configFile.writeText(output)
        println("✅ Set '$key' = '$value'")
    }

    private fun collectValidationIssues(config: Map<String, Any>, issues: MutableList<String>) {
        @Suppress("UNCHECKED_CAST")
        val project = config["project"] as? Map<String, Any>
        if (project == null) issues.add("Missing 'project' section")
        else if (project["name"] == null) issues.add("Missing 'project.name'")

        val plugins = config["plugins"]
        if (plugins != null && plugins !is List<*>) {
            issues.add("'plugins' should be a list")
        }
    }

    private fun collectDeprecatedKeyIssues(config: Map<String, Any>, issues: MutableList<String>) {
        if (config.containsKey("scripts")) {
            issues.add("Deprecated top-level key 'scripts'. Use 'tasks' instead.")
        }
        @Suppress("UNCHECKED_CAST")
        val docs = config["docs"] as? Map<String, Any>
        if (docs?.containsKey("site_name") == true) {
            issues.add("Deprecated key 'docs.site_name'. Use 'docs.siteName' instead.")
        }
    }

    private fun collectTopLevelKeySuggestionIssues(config: Map<String, Any>, issues: MutableList<String>) {
        val knownCoreKeys = setOf("project", "plugins", "affected", "tasks")
        for (key in config.keys) {
            if (key in knownCoreKeys || key == "scripts") continue
            val suggestion = nearestSuggestion(key, knownCoreKeys)
            if (suggestion != null) {
                issues.add("Unknown top-level key '$key'. Did you mean '$suggestion'?")
            }
        }
    }

    private fun collectPluginIssues(config: Map<String, Any>, issues: MutableList<String>) {
        val plugins = config["plugins"] as? List<*> ?: return
        for (entry in plugins) {
            val pluginName = when (entry) {
                is String -> entry
                is Map<*, *> -> entry["name"] as? String
                else -> null
            }
            if (pluginName == null) continue
            if (isKnownPluginVariant(pluginName)) continue

            val suggestion = suggestPluginName(pluginName)
            if (suggestion != null) {
                issues.add("Unknown plugin '$pluginName'. Did you mean '$suggestion'?")
            } else {
                issues.add("Unknown plugin '$pluginName'")
            }
        }
    }

    private fun isKnownPluginVariant(pluginName: String): Boolean {
        val normalized = normalizeToken(pluginName)
        if (normalized in normalizedKnownPluginVariants) return true
        if (normalized.endsWith("context")) {
            val root = normalized.removeSuffix("context")
            if (root in normalizedKnownPluginRoots) return true
        }
        return false
    }

    private fun suggestPluginName(pluginName: String): String? {
        val normalized = normalizeToken(pluginName)
        val nearest = knownPluginIds.minByOrNull { levenshtein(normalized, normalizeToken(it)) }
            ?: return null
        val distance = levenshtein(normalized, normalizeToken(nearest))
        return if (distance <= 6) nearest else null
    }

    private fun nearestSuggestion(value: String, options: Set<String>): String? {
        val nearest = options.minByOrNull { levenshtein(normalizeToken(value), normalizeToken(it)) } ?: return null
        val distance = levenshtein(normalizeToken(value), normalizeToken(nearest))
        return if (distance <= 2) nearest else null
    }

    private fun normalizeToken(value: String): String =
        value
            .lowercase()
            .replace("architected", "")
            .replace("plugin", "")
            .replace("context", "")
            .filter { it.isLetterOrDigit() }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var previous = costs[0]
            costs[0] = i
            for (j in 1..b.length) {
                val temp = costs[j]
                val substitution = previous + if (a[i - 1] == b[j - 1]) 0 else 1
                val insertion = costs[j] + 1
                val deletion = costs[j - 1] + 1
                costs[j] = min(min(insertion, deletion), substitution)
                previous = temp
            }
        }
        return costs[b.length]
    }

    @Suppress("UNCHECKED_CAST")
    private fun getNestedValue(map: Map<String, Any>, keys: List<String>): Any? {
        if (keys.isEmpty()) return null
        val value = map[keys.first()] ?: return null
        return if (keys.size == 1) value
        else if (value is Map<*, *>) getNestedValue(value as Map<String, Any>, keys.drop(1))
        else null
    }

    @Suppress("UNCHECKED_CAST")
    private fun setNestedValue(map: MutableMap<String, Any>, keys: List<String>, value: String) {
        if (keys.size == 1) {
            map[keys.first()] = value
            return
        }
        val nested = map.getOrPut(keys.first()) { mutableMapOf<String, Any>() }
        if (nested is MutableMap<*, *>) {
            setNestedValue(nested as MutableMap<String, Any>, keys.drop(1), value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun printConfigMap(map: Map<String, Any>, indent: Int) {
        for ((key, value) in map) {
            val prefix = " ".repeat(indent)
            when (value) {
                is Map<*, *> -> {
                    println("$prefix$key:")
                    printConfigMap(value as Map<String, Any>, indent + 2)
                }
                is List<*> -> {
                    println("$prefix$key:")
                    value.forEach { item ->
                        if (item is Map<*, *>) {
                            println("$prefix  -")
                            printConfigMap(item as Map<String, Any>, indent + 4)
                        } else {
                            println("$prefix  - $item")
                        }
                    }
                }
                else -> println("$prefix$key: $value")
            }
        }
    }

    companion object {
        private val knownPluginIds = setOf(
            "architecture-architected",
            "docker-architected",
            "docs-architected",
            "git-architected",
            "github-architected",
            "go-architected",
            "gradle-architected",
            "javascript-architected",
            "kubernetes-architected",
            "maven-architected",
            "nx-architected",
            "pipelines-architected",
            "python-architected",
            "quality-architected",
            "release-architected",
            "rust-architected",
            "scripts-architected",
            "security-architected",
            "terraform-architected",
            "testing-architected",
        )

        private val normalizedKnownPluginRoots = knownPluginIds.map { id ->
            normalizeCompanionToken(id)
        }.toSet()

        private val normalizedKnownPluginVariants = buildSet {
            for (id in knownPluginIds) {
                val root = normalizeCompanionToken(id)
                add(root)
                add(normalizeCompanionToken("$root-plugin"))
                add(normalizeCompanionToken("$root context"))
                add(normalizeCompanionToken(id))
            }
        }

        private fun normalizeCompanionToken(value: String): String =
            value
                .lowercase()
                .replace("architected", "")
                .replace("plugin", "")
                .replace("context", "")
                .filter { it.isLetterOrDigit() }
    }
}
