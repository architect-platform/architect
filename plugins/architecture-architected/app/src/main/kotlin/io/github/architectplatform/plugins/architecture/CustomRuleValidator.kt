package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

class CustomRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> {
        val validatorName = rule.validator
        if (validatorName.isNullOrBlank()) {
            return listOf(
                ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    severity = "warning",
                    message = "Custom rule '${rule.id}' has no validator configured",
                    suggestion = "Set architecture.customRules[].validator to a RuleValidator implementation.",
                ),
            )
        }

        val validator = runCatching {
            Class.forName(validatorName).getDeclaredConstructor().newInstance() as? RuleValidator
        }.getOrNull()

        return if (validator == null) {
            listOf(
                ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    severity = "warning",
                    message = "Custom validator '$validatorName' could not be loaded",
                    suggestion = "Ensure the validator class is on the plugin classpath and implements RuleValidator.",
                ),
            )
        } else {
            validator.validate(rule, projectDir, files, importGraph)
        }
    }
}
