package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

class NamingRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> {
        val patternRegex = runCatching { Regex(rule.pattern) }.getOrElse {
            return listOf(
                ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    message = "Invalid pattern regex: ${rule.pattern}",
                ),
            )
        }

        return files.filter { RuleSupport.matchesPaths(rule.paths, it.relativePath) }
            .mapNotNull { analysis ->
                val fileNameWithoutExtension = analysis.file.fileName.toString().substringBeforeLast('.')
                if (patternRegex.matches(fileNameWithoutExtension)) {
                    null
                } else {
                    ArchitectureRules.Violation(
                        rule = rule,
                        file = analysis.file,
                        message = "File name '$fileNameWithoutExtension' does not match required pattern: ${rule.pattern}",
                        suggestion = rule.suggestion ?: "Rename the file or declaration to match the naming convention.",
                    )
                }
            }
    }
}
