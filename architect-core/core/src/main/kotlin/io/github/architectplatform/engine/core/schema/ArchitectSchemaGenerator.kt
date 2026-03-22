package io.github.architectplatform.engine.core.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Generates a JSON Schema (draft-07) for `architect.yml`.
 *
 * Since the config model is a dynamic [Map<String, Any>], this generator builds
 * the schema programmatically from the known structure rather than reflecting
 * over typed classes.
 */
object ArchitectSchemaGenerator {

  private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

  fun generate(): ObjectNode {
    val schema = mapper.createObjectNode()
    schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
    schema.put("\$id", "https://architect.dev/schema/architect.yml.json")
    schema.put("title", "Architect Configuration")
    schema.put("description", "Configuration schema for architect.yml — the Architect Platform project descriptor.")
    schema.put("type", "object")

    val properties = schema.putObject("properties")
    properties.set<ObjectNode>("\$schema", stringProp("JSON Schema URL for editor validation and auto-complete"))
    properties.set<ObjectNode>("project", projectSchema())
    properties.set<ObjectNode>("plugins", pluginsSchema())
    properties.set<ObjectNode>("tasks", tasksSchema())

    schema.putArray("required").add("project")
    schema.put("additionalProperties", true)

    val defs = schema.putObject("definitions")
    defs.set<ObjectNode>("pluginConfig", pluginConfigDef())
    defs.set<ObjectNode>("inlineTask", inlineTaskDef())

    return schema
  }

  fun generateJson(): String = mapper.writeValueAsString(generate())

  // ── Sections ──────────────────────────────────────────────────────────

  private fun projectSchema(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "object")
    node.put("description", "Project metadata.")
    val props = node.putObject("properties")
    props.set<ObjectNode>("name", stringProp("Unique project name (used as identifier)"))
    props.set<ObjectNode>("description", stringProp("Human-readable project description"))
    props.set<ObjectNode>("affected", affectedSchema())
    node.putArray("required").add("name")
    node.put("additionalProperties", false)
    return node
  }

  private fun affectedSchema(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "object")
    node.put("description", "Configuration for affected project detection in monorepos.")
    val props = node.putObject("properties")
    props.set<ObjectNode>("always-include", stringArrayProp("Projects to always include in affected runs, regardless of changes"))
    props.set<ObjectNode>("never-include", stringArrayProp("Projects to always exclude from affected runs"))
    node.put("additionalProperties", false)
    return node
  }

  private fun pluginsSchema(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "array")
    node.put("description", "List of plugins to load for this project.")
    node.putObject("items").put("\$ref", "#/definitions/pluginConfig")
    return node
  }

  private fun tasksSchema(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "object")
    node.put("description", "Inline task definitions. Keys are task IDs.")
    node.putObject("additionalProperties").put("\$ref", "#/definitions/inlineTask")
    return node
  }

  // ── Definitions ───────────────────────────────────────────────────────

  private fun pluginConfigDef(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "object")
    node.put("description", "Plugin source declaration.")

    val props = node.putObject("properties")
    props.set<ObjectNode>("name", stringProp("Plugin identifier (e.g. git-architected)"))
    props.set<ObjectNode>("version", stringPropWithDefault("Semver constraint or 'latest'", "latest"))
    props.set<ObjectNode>("type", pluginTypeEnum())
    props.set<ObjectNode>("repo", stringProp("GitHub repository in owner/name format"))
    props.set<ObjectNode>("owner", stringPropWithDefault("GitHub repository owner", "architect-platform"))
    props.set<ObjectNode>("asset", stringProp("Plugin asset filename"))
    props.set<ObjectNode>(
      "assetType",
      stringEnumProp("Asset file type", listOf("jar"), "jar"),
    )
    props.set<ObjectNode>("path", stringPropWithDefault("Local path to the plugin (for type: local)", "."))
    props.set<ObjectNode>("pattern", stringProp("Release asset filename prefix pattern"))
      props.set<ObjectNode>("registry", stringProp("Registry index URL for type: registry"))
      props.set<ObjectNode>("url", stringProp("Direct plugin asset URL for type: http"))
    props.set<ObjectNode>("command", stringProp("Command to execute for type: process"))
      props.set<ObjectNode>("package", stringProp("npm package name for type: npm"))

    node.putArray("required").add("name")
    node.set<ArrayNode>("allOf", pluginTypeRequirements())
    node.put("additionalProperties", false)
    return node
  }

  private fun inlineTaskDef(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "object")
    node.put("description", "An inline task defined directly in architect.yml.")

    val props = node.putObject("properties")
    props.set<ObjectNode>("description", stringProp("Human-readable description of the task"))
    props.set<ObjectNode>("run", stringProp("Shell command to execute"))
    props.set<ObjectNode>("phase", phaseEnum())
    props.set<ObjectNode>("depends", stringArrayProp("Task IDs this task depends on"))

    node.put("additionalProperties", false)
    return node
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private fun stringProp(description: String): ObjectNode =
    mapper.createObjectNode().put("type", "string").put("description", description)

  private fun stringPropWithDefault(description: String, default: String): ObjectNode =
    stringProp(description).put("default", default)

  private fun stringArrayProp(description: String): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "array")
    node.put("description", description)
    node.putObject("items").put("type", "string")
    return node
  }

  private fun stringEnumProp(description: String, values: List<String>, default: String): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "string")
    node.put("description", description)
    val arr = node.putArray("enum")
    values.forEach { arr.add(it) }
    node.put("default", default)
    return node
  }

  private fun pluginTypeEnum(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "string")
    node.put("description", "Plugin source type")
    val arr = node.putArray("enum")
    listOf("github", "local", "registry", "http", "process", "npm").forEach { arr.add(it) }
    node.put("default", "github")
    return node
  }

  private fun pluginTypeRequirements(): ArrayNode {
    val allOf = mapper.createArrayNode()
    allOf.add(typeRequirement(type = "local", field = "path"))
    allOf.add(typeRequirement(type = "http", field = "url"))
    allOf.add(typeRequirement(type = "registry", field = "registry"))
    allOf.add(typeRequirement(type = "process", field = "command"))
    allOf.add(typeRequirement(type = "npm", field = "package"))
    return allOf
  }

  private fun typeRequirement(type: String, field: String): ObjectNode {
    val node = mapper.createObjectNode()
    val ifNode = node.putObject("if")
    ifNode.putObject("properties").putObject("type").put("const", type)
    node.putObject("then").putArray("required").add(field)
    return node
  }

  private fun phaseEnum(): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", "string")
    node.put("description", "Lifecycle phase this task belongs to")
    val arr = node.putArray("enum")
    // CoreWorkflow phases
    listOf("INIT", "LINT", "VERIFY", "BUILD", "RUN", "TEST", "RELEASE", "PUBLISH").forEach { arr.add(it) }
    // CodeWorkflow phases
    listOf(
      "CODE-init", "CODE-lint", "CODE-verify", "CODE-build",
      "CODE-run", "CODE-test", "CODE-release", "CODE-publish",
    ).forEach { arr.add(it) }
    // HooksWorkflow phases
    listOf("pre-commit", "pre-push", "commit-msg").forEach { arr.add(it) }
    return node
  }
}
