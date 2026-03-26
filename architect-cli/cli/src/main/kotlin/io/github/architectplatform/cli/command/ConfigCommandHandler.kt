package io.github.architectplatform.cli.command

/**
 * Handles `architect config` — configuration management subcommands.
 *
 * Subcommands:
 * - `architect config show`     — display current resolved configuration
 * - `architect config validate` — validate config against schema
 * - `architect config get key`  — get a config value by dotted key
 * - `architect config set key value` — set a config value by dotted key
 */
class ConfigCommandHandler {

    var json: Boolean = false
    var plain: Boolean = false

    fun handle(args: List<String>) {
        val subcommand = args.getOrNull(1) ?: "show"
        when (subcommand) {
            "show" -> handleShow()
            "validate" -> handleValidate()
            "get" -> handleGet(args.getOrNull(2))
            "set" -> handleSet(args.getOrNull(2), args.getOrNull(3))
            else -> {
                println("Unknown config subcommand: $subcommand")
                println("Available: show, validate, get, set")
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
}
