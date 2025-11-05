package io.github.architectplatform.plugins.architecture

/**
 * Configuration context for the Architecture plugin.
 *
 * Contains all settings for architectural rule management and validation.
 *
 * Example configuration:
 * ```yaml
 * architecture:
 *   enabled: true
 *   rulesets:
 *     layered:
 *       enabled: true
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
 *       rules:
 *         - id: "controller-naming"
 *           description: "Controllers must end with Controller"
 *           type: "naming"
 *           pattern: ".*Controller"
 *           paths: ["src/main/.*Controller\\..*"]
 *   customRules:
 *     - id: "custom-rule-1"
 *       description: "Custom architectural rule"
 *       type: "custom"
 *       validator: "io.example.CustomValidator"
 *   onViolation: "warn"  # or "fail"
 *   reportFormat: "text"  # or "json", "html"
 * ```
 *
 * @property enabled Whether the architecture plugin is enabled
 * @property rulesets Map of ruleset name to ruleset configuration
 * @property customRules List of custom rules defined by the user
 * @property onViolation Action to take on rule violation: "warn" or "fail"
 * @property reportFormat Format for validation report: "text", "json", or "html"
 * @property strict If true, treats warnings as errors
 */
data class ArchitectureContext(
    val enabled: Boolean = true,
    val rulesets: Map<String, RuleSet> = emptyMap(),
    val customRules: List<ArchitectureRule> = emptyList(),
    val onViolation: String = "warn",
    val reportFormat: String = "text",
    val strict: Boolean = false
)

/**
 * A ruleset is a collection of related architectural rules.
 *
 * @property enabled Whether this ruleset is enabled
 * @property description Description of the ruleset
 * @property rules List of rules in this ruleset
 */
data class RuleSet(
    val enabled: Boolean = true,
    val description: String = "",
    val rules: List<ArchitectureRule> = emptyList()
)

/**
 * An architectural rule that can be validated.
 *
 * @property id Unique identifier for the rule
 * @property description Human-readable description of the rule
 * @property type Type of rule: "dependency", "naming", "structure", "custom"
 * @property pattern Pattern to match for the rule (regex)
 * @property paths Optional list of file path patterns to apply this rule to
 * @property forbidden For dependency rules: list of forbidden dependencies (regex patterns)
 * @property required For dependency rules: list of required dependencies (regex patterns)
 * @property validator For custom rules: fully qualified class name of custom validator
 * @property severity Severity level: "error", "warning", "info"
 * @property enabled Whether this rule is enabled
 */
data class ArchitectureRule(
    val id: String,
    val description: String = "",
    val type: String = "dependency",
    val pattern: String = ".*",
    val paths: List<String> = emptyList(),
    val forbidden: List<String> = emptyList(),
    val required: List<String> = emptyList(),
    val validator: String? = null,
    val severity: String = "error",
    val enabled: Boolean = true
)
