package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

class DependencyRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> {
        val ruleRegex = runCatching { Regex(rule.pattern) }.getOrElse {
            return listOf(
                ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    message = "Invalid pattern regex: ${rule.pattern}",
                ),
            )
        }

        return files.filter { RuleSupport.matchesPaths(rule.paths, it.relativePath) }
            .filter { analysis ->
                val fileName = analysis.file.fileName.toString().substringBeforeLast('.')
                ruleRegex.matches(fileName) || analysis.declarations.any { declaration -> ruleRegex.matches(declaration.name) }
            }
            .flatMap { analysis ->
                val violations = mutableListOf<ArchitectureRules.Violation>()
                rule.forbidden.forEach { forbiddenPattern ->
                    val forbiddenRegex = runCatching { Regex(forbiddenPattern) }.getOrNull() ?: return@forEach
                    analysis.imports.filter { forbiddenRegex.matches(it.name) }.forEach { importReference ->
                        violations += ArchitectureRules.Violation(
                            rule = rule,
                            file = analysis.file,
                            line = importReference.line,
                            message = "File contains forbidden dependency: ${importReference.name} (matched pattern: $forbiddenPattern)",
                            suggestion = rule.suggestion ?: "Depend on an interface or lower-level abstraction instead.",
                        )
                    }
                }
                rule.required.forEach { requiredPattern ->
                    val requiredRegex = runCatching { Regex(requiredPattern) }.getOrNull() ?: return@forEach
                    if (analysis.imports.none { requiredRegex.matches(it.name) }) {
                        violations += ArchitectureRules.Violation(
                            rule = rule,
                            file = analysis.file,
                            message = "File is missing required dependency matching pattern: $requiredPattern",
                            suggestion = rule.suggestion ?: "Add the missing dependency or relax the rule requirements.",
                        )
                    }
                }
                violations
            }
    }
}
