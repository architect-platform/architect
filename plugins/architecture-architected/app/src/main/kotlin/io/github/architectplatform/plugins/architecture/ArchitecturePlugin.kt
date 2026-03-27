package io.github.architectplatform.plugins.architecture

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Architect plugin for architectural rule management and validation.
 *
 * This plugin enables projects to define and enforce architectural rules,
 * ensuring code consistency with the adopted architectural patterns and templates.
 *
 * Features:
 * - Define custom rulesets for architectural validation
 * - Support for dependency rules (forbidden/required dependencies)
 * - Support for naming conventions
 * - Support for structural rules
 * - Extensible with custom validators
 * - Multiple report formats (text, JSON, HTML)
 * - Configurable violation handling (warn/fail)
 *
 * Example configuration:
 * ```yaml
 * architecture:
 *   enabled: true
 *   rulesets:
 *     layered:
 *       enabled: true
 *       description: "Layered architecture rules"
 *       rules:
 *         - id: "no-direct-db-access"
 *           description: "Controllers should not directly access database"
 *           type: "dependency"
 *           pattern: ".*Controller.*"
 *           forbidden: [".*Repository.*", ".*DAO.*"]
 *         - id: "service-layer-required"
 *           description: "Controllers must use service layer"
 *           type: "dependency"
 *           pattern: ".*Controller.*"
 *           required: [".*Service.*"]
 *     naming:
 *       enabled: true
 *       description: "Naming convention rules"
 *       rules:
 *         - id: "controller-naming"
 *           description: "Controllers must end with Controller"
 *           type: "naming"
 *           pattern: ".*Controller"
 *           paths: ["src/main/.*Controller\\..*"]
 *   onViolation: "warn"  # or "fail"
 *   reportFormat: "text"  # or "json", "html"
 *   strict: false
 * ```
 *
 * Tasks:
 * - architecture-validate: Validates the project against defined architectural rules
 */
class ArchitecturePlugin : ArchitectPlugin<ArchitectureContext> {
    override val id = "architecture-plugin"
    override val contextKey: String = "architecture"
    override val ctxClass: Class<ArchitectureContext> = ArchitectureContext::class.java
    override var context: ArchitectureContext = ArchitectureContext()

    override fun configSchema(): Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
            "enabled" to mapOf("type" to "boolean", "default" to true),
            "presetRulesets" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "string",
                    "enum" to BuiltInArchitectureRulesets.ids().sorted(),
                ),
                "default" to emptyList<String>(),
            ),
            "rulesets" to mapOf(
                "type" to "object",
                "additionalProperties" to mapOf("\$ref" to "#/\$defs/ruleSet"),
                "default" to emptyMap<String, Any>(),
            ),
            "customRules" to mapOf(
                "type" to "array",
                "items" to mapOf("\$ref" to "#/\$defs/rule"),
                "default" to emptyList<Any>(),
            ),
            "structure" to mapOf(
                "\$ref" to "#/\$defs/structure",
                "default" to emptyMap<String, Any>(),
            ),
            "onViolation" to mapOf(
                "type" to "string",
                "enum" to listOf("warn", "fail"),
                "default" to "warn",
            ),
            "reportFormat" to mapOf(
                "type" to "string",
                "enum" to listOf("text", "json"),
                "default" to "text",
            ),
            "strict" to mapOf("type" to "boolean", "default" to false),
        ),
        "\$defs" to mapOf(
            "ruleSet" to mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "properties" to mapOf(
                    "enabled" to mapOf("type" to "boolean", "default" to true),
                    "description" to mapOf("type" to "string", "default" to ""),
                    "rules" to mapOf(
                        "type" to "array",
                        "items" to mapOf("\$ref" to "#/\$defs/rule"),
                        "default" to emptyList<Any>(),
                    ),
                ),
            ),
            "rule" to mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "required" to listOf("id"),
                "properties" to mapOf(
                    "id" to mapOf("type" to "string"),
                    "description" to mapOf("type" to "string", "default" to ""),
                    "type" to mapOf(
                        "type" to "string",
                        "enum" to listOf("dependency", "naming", "structure", "import", "convention", "custom"),
                        "default" to "dependency",
                    ),
                    "pattern" to mapOf("type" to "string", "default" to ".*"),
                    "paths" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "default" to emptyList<String>(),
                    ),
                    "forbidden" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "default" to emptyList<String>(),
                    ),
                    "required" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "default" to emptyList<String>(),
                    ),
                    "validator" to mapOf("type" to "string"),
                    "convention" to mapOf(
                        "type" to "string",
                        "enum" to listOf("kdoc-required", "javadoc-required", "test-class-exists"),
                    ),
                    "threshold" to mapOf("type" to "integer", "minimum" to 0),
                    "allowedCycles" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "default" to emptyList<String>(),
                    ),
                    "moduleBoundaries" to mapOf(
                        "type" to "object",
                        "additionalProperties" to mapOf(
                            "type" to "array",
                            "items" to mapOf("type" to "string"),
                        ),
                        "default" to emptyMap<String, Any>(),
                    ),
                    "suggestion" to mapOf("type" to "string"),
                    "severity" to mapOf(
                        "type" to "string",
                        "enum" to listOf("error", "warning", "info"),
                        "default" to "error",
                    ),
                    "enabled" to mapOf("type" to "boolean", "default" to true),
                ),
            ),
            "structure" to mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "properties" to mapOf(
                    "enabled" to mapOf("type" to "boolean", "default" to true),
                    "required" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "default" to emptyList<String>(),
                    ),
                    "forbidden" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "default" to emptyList<String>(),
                    ),
                ),
            ),
        ),
    )

    override fun register(registry: TaskRegistry) {
        registry.add(ArchitectureTask(CodeWorkflow.VERIFY, context))
    }
}
