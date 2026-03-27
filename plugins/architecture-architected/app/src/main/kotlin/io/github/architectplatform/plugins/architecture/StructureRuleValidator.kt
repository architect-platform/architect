package io.github.architectplatform.plugins.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class StructureRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> {
        val violations = mutableListOf<ArchitectureRules.Violation>()
        val allPaths = Files.walk(projectDir).use { stream ->
            stream.map { projectDir.relativize(it).toString().replace('\\', '/') }
                .filter { it.isNotBlank() }
                .toList()
        }

        rule.paths.forEach { pathPattern ->
            val basePath = pathPattern.substringBefore('*').trimEnd('/')
            val hasMatch = allPaths.any { RuleSupport.globMatches(pathPattern, it) }
            val baseExists = basePath.isBlank() || projectDir.resolve(basePath).exists()
            if (!hasMatch && !baseExists) {
                violations += ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir,
                    message = "Required path does not exist: $pathPattern",
                    suggestion = rule.suggestion ?: "Create the missing path or update the structure rule.",
                )
            }
        }

        rule.forbidden.forEach { forbiddenPattern ->
            allPaths.filter { RuleSupport.globMatches(forbiddenPattern, it) }.forEach { matchedPath ->
                violations += ArchitectureRules.Violation(
                    rule = rule,
                    file = projectDir.resolve(matchedPath),
                    message = "Forbidden path detected: $matchedPath",
                    suggestion = rule.suggestion ?: "Remove the forbidden path or update the structure policy.",
                )
            }
        }

        return violations
    }
}
