package io.github.architectplatform.plugins.architecture

data class ArchitectureContext(
    val enabled: Boolean = true,
    val presetRulesets: List<String> = emptyList(),
    val rulesets: Map<String, RuleSet> = emptyMap(),
    val customRules: List<ArchitectureRule> = emptyList(),
    val structure: ArchitectureStructure = ArchitectureStructure(),
    val boundaries: Map<String, List<String>> = emptyMap(),
    val onViolation: String = "warn",
    val reportFormat: String = "text",
    val strict: Boolean = false,
) {
    fun normalizedReportFormat(): String = reportFormat.trim().lowercase()

    fun resolvedRulesets(): Map<String, RuleSet> {
        val resolved = linkedMapOf<String, RuleSet>()

        presetRulesets.forEach { presetId ->
            BuiltInArchitectureRulesets.find(presetId)?.let { resolved[presetId] = it }
        }

        rulesets.forEach { (name, ruleset) ->
            val builtIn = BuiltInArchitectureRulesets.find(name)
            resolved[name] =
                if (builtIn != null && ruleset.rules.isEmpty()) {
                    builtIn.copy(
                        enabled = ruleset.enabled,
                        description = ruleset.description.ifBlank { builtIn.description },
                    )
                } else {
                    ruleset
                }
        }

        return resolved
    }
}

data class ArchitectureStructure(
    val enabled: Boolean = true,
    val required: List<String> = emptyList(),
    val forbidden: List<String> = emptyList(),
)

data class RuleSet(
    val enabled: Boolean = true,
    val description: String = "",
    val rules: List<ArchitectureRule> = emptyList(),
)

data class ArchitectureRule(
    val id: String,
    val description: String = "",
    val type: String = "dependency",
    val pattern: String = ".*",
    val paths: List<String> = emptyList(),
    val forbidden: List<String> = emptyList(),
    val required: List<String> = emptyList(),
    val validator: String? = null,
    val convention: String? = null,
    val threshold: Int? = null,
    val allowedCycles: List<String> = emptyList(),
    val moduleBoundaries: Map<String, List<String>> = emptyMap(),
    val suggestion: String? = null,
    val severity: String = "error",
    val enabled: Boolean = true,
) {
    fun normalizedType(): String = type.trim().lowercase()
}
