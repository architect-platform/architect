package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

class ArchitectureRules(
    private val context: ArchitectureContext,
    private val ruleRegistry: RuleRegistry = RuleRegistry.default(),
    private val importGraphBuilder: ImportGraphBuilder = ImportGraphBuilder(),
) {
    data class Violation(
        val rule: ArchitectureRule,
        val file: Path,
        val line: Int? = null,
        val message: String,
        val severity: String = rule.severity,
        val suggestion: String? = rule.suggestion,
    )

    data class ValidationResult(
        val violations: List<Violation>,
        val totalRulesChecked: Int,
        val filesAnalyzed: Int,
    ) {
        val hasErrors: Boolean
            get() = violations.any { it.severity == "error" }

        val hasWarnings: Boolean
            get() = violations.any { it.severity == "warning" }

        fun shouldFail(strict: Boolean, onViolation: String): Boolean =
            hasErrors || (strict && hasWarnings) || (onViolation == "fail" && violations.isNotEmpty())
    }

    fun validate(projectDir: Path, includedTypes: Set<String>? = null): ValidationResult {
        if (!context.enabled) {
            return ValidationResult(emptyList(), 0, 0)
        }

        val enabledRules = getAllRules()
            .filter { it.enabled }
            .filter { includedTypes == null || it.normalizedType() in includedTypes }
        if (enabledRules.isEmpty()) {
            return ValidationResult(emptyList(), 0, 0)
        }

        val analyses = RuleSupport.analyzeProjectFiles(projectDir)
        val importGraph = importGraphBuilder.build(analyses)
        val violations = enabledRules.flatMap { rule ->
            ruleRegistry.validatorFor(rule)?.validate(rule, projectDir, analyses, importGraph)
                ?: listOf(
                    Violation(
                        rule = rule,
                        file = projectDir,
                        severity = "warning",
                        message = "Unknown rule type '${rule.type}'",
                        suggestion = "Use one of: dependency, naming, structure, import, convention, custom.",
                    ),
                )
        }

        return ValidationResult(
            violations = violations,
            totalRulesChecked = enabledRules.size,
            filesAnalyzed = analyses.size,
        )
    }

    fun formatTextReport(result: ValidationResult): String {
        val report = StringBuilder()
        report.appendLine("=".repeat(80))
        report.appendLine("Architecture Validation Report")
        report.appendLine("=".repeat(80))
        report.appendLine()
        report.appendLine("Summary:")
        report.appendLine("  Rules checked: ${result.totalRulesChecked}")
        report.appendLine("  Files analyzed: ${result.filesAnalyzed}")
        report.appendLine("  Violations found: ${result.violations.size}")
        report.appendLine()

        if (result.violations.isEmpty()) {
            report.appendLine("✓ No violations found. All architectural rules are satisfied.")
        } else {
            report.appendLine("Violations:")
            report.appendLine()

            result.violations.groupBy { it.rule.id }.forEach { (ruleId, violations) ->
                val rule = violations.first().rule
                report.appendLine("Rule: $ruleId [${violations.first().severity.uppercase()}]")
                report.appendLine("  Description: ${rule.description}")
                report.appendLine("  Violations: ${violations.size}")
                report.appendLine()

                violations.forEach { violation ->
                    val location = buildString {
                        append(violation.file)
                        violation.line?.let { append(":$it") }
                    }
                    report.appendLine("    • $location")
                    report.appendLine("      ${violation.message}")
                    violation.suggestion?.let { report.appendLine("      Suggestion: $it") }
                }
                report.appendLine()
            }
        }

        report.appendLine("=".repeat(80))
        return report.toString()
    }

    fun formatJsonReport(result: ValidationResult): String {
        val json = StringBuilder()
        json.appendLine("{")
        json.appendLine("  \"summary\": {")
        json.appendLine("    \"rulesChecked\": ${result.totalRulesChecked},")
        json.appendLine("    \"filesAnalyzed\": ${result.filesAnalyzed},")
        json.appendLine("    \"violationsFound\": ${result.violations.size},")
        json.appendLine("    \"hasErrors\": ${result.hasErrors},")
        json.appendLine("    \"hasWarnings\": ${result.hasWarnings}")
        json.appendLine("  },")
        json.appendLine("  \"violations\": [")

        result.violations.forEachIndexed { index, violation ->
            json.appendLine("    {")
            json.appendLine("      \"rule\": \"${escape(violation.rule.id)}\",")
            json.appendLine("      \"severity\": \"${escape(violation.severity)}\",")
            json.appendLine("      \"file\": \"${escape(violation.file.toString())}\",")
            json.appendLine("      \"line\": ${violation.line},")
            json.appendLine("      \"message\": \"${escape(violation.message)}\",")
            json.appendLine("      \"suggestion\": ${violation.suggestion?.let { "\"${escape(it)}\"" } ?: "null"}")
            json.append("    }")
            if (index < result.violations.lastIndex) {
                json.appendLine(",")
            } else {
                json.appendLine()
            }
        }

        json.appendLine("  ]")
        json.appendLine("}")
        return json.toString()
    }

    private fun getAllRules(): List<ArchitectureRule> {
        val rules = mutableListOf<ArchitectureRule>()
        context.resolvedRulesets().values.filter { it.enabled }.forEach { rules += it.rules }
        rules += structureRules()
        rules += boundaryRules()
        rules += context.customRules
        return rules
    }

    private fun structureRules(): List<ArchitectureRule> {
        if (!context.structure.enabled) {
            return emptyList()
        }

        val derivedRules = mutableListOf<ArchitectureRule>()
        if (context.structure.required.isNotEmpty()) {
            derivedRules += ArchitectureRule(
                id = "configured-structure-required",
                description = "Required project structure from architecture.structure.required",
                type = "structure",
                paths = context.structure.required,
                suggestion = "Create the missing path or adjust architecture.structure.required.",
            )
        }
        if (context.structure.forbidden.isNotEmpty()) {
            derivedRules += ArchitectureRule(
                id = "configured-structure-forbidden",
                description = "Forbidden project structure from architecture.structure.forbidden",
                type = "structure",
                forbidden = context.structure.forbidden,
                suggestion = "Remove the forbidden path or relax architecture.structure.forbidden.",
            )
        }
        return derivedRules
    }

    private fun boundaryRules(): List<ArchitectureRule> {
        if (context.boundaries.isEmpty()) {
            return emptyList()
        }

        return listOf(
            ArchitectureRule(
                id = "configured-module-boundaries",
                description = "Monorepo dependency boundaries from architecture.boundaries",
                type = "import",
                moduleBoundaries = context.boundaries,
                suggestion = "Move shared APIs into an allowed module or relax architecture.boundaries.",
            )
        )
    }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
