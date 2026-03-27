package io.github.architectplatform.plugins.architecture

object BuiltInArchitectureRulesets {
    private val rulesets = mapOf(
        "layered-architecture" to RuleSet(
            description = "Controllers depend on services, services depend on repositories.",
            rules = listOf(
                ArchitectureRule(
                    id = "layered-no-controller-repository-import",
                    description = "Controllers must not import repositories directly",
                    type = "dependency",
                    pattern = ".*Controller.*",
                    forbidden = listOf(".*Repository.*"),
                    suggestion = "Inject a service layer between controllers and repositories.",
                ),
                ArchitectureRule(
                    id = "layered-controller-requires-service",
                    description = "Controllers should depend on services",
                    type = "dependency",
                    pattern = ".*Controller.*",
                    required = listOf(".*Service.*"),
                    severity = "warning",
                    suggestion = "Move orchestration logic into a service and import that service from controllers.",
                ),
                ArchitectureRule(
                    id = "layered-service-no-controller-import",
                    description = "Services must not depend on controllers",
                    type = "dependency",
                    pattern = ".*Service.*",
                    forbidden = listOf(".*Controller.*"),
                    suggestion = "Move shared contracts out of controller packages.",
                ),
            ),
        ),
        "hexagonal-architecture" to RuleSet(
            description = "Domain and ports stay isolated from adapters.",
            rules = listOf(
                ArchitectureRule(
                    id = "hexagonal-domain-no-adapters",
                    description = "Domain code must not depend on adapters or frameworks",
                    type = "dependency",
                    pattern = ".*(Domain|Entity|Aggregate|UseCase).*",
                    forbidden = listOf(".*(Controller|Adapter|Rest|Persistence|Jpa).*"),
                    suggestion = "Move framework code into adapters and depend on ports from the domain layer.",
                ),
                ArchitectureRule(
                    id = "hexagonal-port-naming",
                    description = "Ports should be explicitly named",
                    type = "naming",
                    pattern = ".*(Port|UseCase|Adapter|Domain|Entity)",
                    paths = listOf("src/main/.*\\.kt", "src/main/.*\\.java"),
                    severity = "warning",
                    suggestion = "Use names that communicate whether a type is a port, adapter, or domain type.",
                ),
            ),
        ),
        "clean-architecture" to RuleSet(
            description = "Entities and use cases remain independent from interface/framework layers.",
            rules = listOf(
                ArchitectureRule(
                    id = "clean-entities-no-frameworks",
                    description = "Entities and use cases must not depend on frameworks",
                    type = "dependency",
                    pattern = ".*(Entity|UseCase|Interactor).*",
                    forbidden = listOf(".*(Controller|View|Rest|Http|Jpa|Sql).*"),
                    suggestion = "Introduce boundary interfaces and move framework integrations outward.",
                ),
                ArchitectureRule(
                    id = "clean-public-kdoc",
                    description = "Public Kotlin APIs should be documented",
                    type = "convention",
                    convention = "kdoc-required",
                    paths = listOf("src/main/.*\\.kt"),
                    severity = "warning",
                    suggestion = "Add KDoc above public declarations to document architectural intent.",
                ),
            ),
        ),
        "monorepo-conventions" to RuleSet(
            description = "Shared modules stay cycle-free and each module contains tests.",
            rules = listOf(
                ArchitectureRule(
                    id = "monorepo-no-import-cycles",
                    description = "Modules must not participate in circular imports",
                    type = "import",
                    suggestion = "Extract shared code into a lower-level module to break the cycle.",
                ),
                ArchitectureRule(
                    id = "monorepo-module-tests",
                    description = "Production classes should have corresponding tests",
                    type = "convention",
                    convention = "test-class-exists",
                    paths = listOf("src/main/.*\\.(kt|java)"),
                    severity = "warning",
                    suggestion = "Add a matching Test class under src/test for each production type.",
                ),
            ),
        ),
    )

    fun ids(): Set<String> = rulesets.keys

    fun find(id: String): RuleSet? = rulesets[id]
}
