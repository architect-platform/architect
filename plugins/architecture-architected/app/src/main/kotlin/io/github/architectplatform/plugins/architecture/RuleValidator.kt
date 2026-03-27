package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

interface RuleValidator {
    fun validate(
        rule: ArchitectureRule,
        projectDir: Path,
        files: List<SourceFileAnalysis>,
        importGraph: ImportGraph,
    ): List<ArchitectureRules.Violation>
}
