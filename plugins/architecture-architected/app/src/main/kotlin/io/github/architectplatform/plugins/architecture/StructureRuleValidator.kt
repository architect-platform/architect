package io.github.architectplatform.plugins.architecture

import java.nio.file.Files
import java.nio.file.Path

class StructureRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> =
        rule.paths.mapNotNull { pathPattern ->
            val resolved = projectDir.resolve(pathPattern)
            if (Files.exists(resolved)) {
                null
            } else {
                ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    message = "Required path does not exist: $pathPattern",
                    suggestion = rule.suggestion ?: "Create the missing path or update the structure rule.",
                )
            }
        }
}
