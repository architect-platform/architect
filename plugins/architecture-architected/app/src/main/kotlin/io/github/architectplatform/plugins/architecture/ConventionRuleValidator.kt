package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

class ConventionRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> =
        when (rule.convention?.trim()?.lowercase() ?: "kdoc-required") {
            "kdoc-required" -> validateDocumentationRule(rule, files, "kt", "KDoc")
            "javadoc-required" -> validateDocumentationRule(rule, files, "java", "Javadoc")
            "test-class-exists" -> validateTestClassExists(rule, files)
            else -> listOf(
                ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    severity = "warning",
                    message = "Unsupported convention '${rule.convention}'",
                    suggestion = "Use one of: kdoc-required, javadoc-required, test-class-exists.",
                ),
            )
        }

    private fun validateDocumentationRule(
        rule: ArchitectureRule,
        files: List<SourceFileAnalysis>,
        extension: String,
        documentationName: String,
    ): List<ArchitectureRules.Violation> =
        files.filter { it.file.fileName.toString().endsWith(".$extension") }
            .filter { RuleSupport.matchesPaths(rule.paths, it.relativePath) }
            .flatMap { analysis ->
                analysis.declarations.filter { it.isPublic && (it.kind == "class" || it.kind == "function" || it.kind == "interface") }
                    .filterNot { RuleSupport.hasDocumentation(analysis.content, it.line) }
                    .map { declaration ->
                        ArchitectureRules.Violation(
                            rule = rule,
                            file = analysis.file,
                            line = declaration.line,
                            message = "Public ${declaration.kind} '${declaration.name}' is missing $documentationName documentation",
                            suggestion = rule.suggestion ?: "Add a $documentationName comment above the declaration.",
                        )
                    }
            }

    private fun validateTestClassExists(
        rule: ArchitectureRule,
        files: List<SourceFileAnalysis>,
    ): List<ArchitectureRules.Violation> =
        files.filter { it.relativePath.contains("/main/") }
            .filter { RuleSupport.matchesPaths(rule.paths, it.relativePath) }
            .filter { !RuleSupport.testFileExists(it, files) }
            .map { analysis ->
                val firstDeclaration = analysis.declarations.firstOrNull()
                ArchitectureRules.Violation(
                    rule = rule,
                    file = analysis.file,
                    line = firstDeclaration?.line,
                    message = "No matching test class found for ${firstDeclaration?.name ?: analysis.file.fileName}",
                    suggestion = rule.suggestion ?: "Create a matching Test file under src/test for the production type.",
                )
            }
}
