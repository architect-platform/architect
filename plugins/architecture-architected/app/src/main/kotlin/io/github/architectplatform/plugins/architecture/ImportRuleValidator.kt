package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

class ImportRuleValidator : RuleValidator {
    override fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation> {
        val violations = mutableListOf<ArchitectureRules.Violation>()
        val relevantFiles = files.filter { RuleSupport.matchesRule(rule, it) || (rule.paths.isEmpty() && rule.pattern == ".*") }
        val relevantPaths = relevantFiles.map { it.relativePath }.toSet()

        importGraph.cycles.forEach { cycle ->
            if (cycle.none { it.relativePath in relevantPaths } && relevantPaths.isNotEmpty()) {
                return@forEach
            }
            val cycleLabel = cycle.joinToString(" -> ") { it.relativePath }
            val allowed = rule.allowedCycles.any { RuleSupport.globMatches(it, cycleLabel) || cycle.any { file -> RuleSupport.globMatches(it, file.relativePath) } }
            if (!allowed) {
                cycle.forEach { file ->
                    violations += ArchitectureRules.Violation(
                        rule = rule,
                        file = file.file,
                        message = "Circular import detected: $cycleLabel",
                        suggestion = rule.suggestion ?: "Break the cycle by extracting shared contracts into a lower-level package or module.",
                    )
                }
            }
        }

        if (rule.moduleBoundaries.isNotEmpty()) {
            importGraph.edges.forEach { edge ->
                if (relevantPaths.isNotEmpty() && edge.from.relativePath !in relevantPaths) {
                    return@forEach
                }
                val allowedModules = RuleSupport.moduleBoundariesFor(rule.moduleBoundaries, edge.from.moduleName) ?: return@forEach
                if (edge.from.moduleName == edge.to.moduleName) {
                    return@forEach
                }
                val allowed = allowedModules.any { RuleSupport.globMatches(it, edge.to.moduleName) }
                if (!allowed) {
                    violations += ArchitectureRules.Violation(
                        rule = rule,
                        file = edge.from.file,
                        line = edge.importReference.line,
                        message = "Cross-module import violation: ${edge.from.moduleName} imports ${edge.to.moduleName} via ${edge.importReference.name}",
                        suggestion = rule.suggestion ?: "Adjust module boundaries or move shared APIs into an allowed dependency module.",
                    )
                }
            }
        }

        return violations
    }
}
