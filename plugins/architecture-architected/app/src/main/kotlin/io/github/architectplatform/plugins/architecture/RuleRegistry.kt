package io.github.architectplatform.plugins.architecture

class RuleRegistry private constructor(
    private val validators: MutableMap<String, RuleValidator>,
) {
    fun register(type: String, validator: RuleValidator) {
        validators[type.trim().lowercase()] = validator
    }

    fun validatorFor(rule: ArchitectureRule): RuleValidator? = validators[rule.normalizedType()]

    companion object {
        fun default(): RuleRegistry {
            val registry = RuleRegistry(linkedMapOf())
            registry.register("dependency", DependencyRuleValidator())
            registry.register("naming", NamingRuleValidator())
            registry.register("structure", StructureRuleValidator())
            registry.register("import", ImportRuleValidator())
            registry.register("convention", ConventionRuleValidator())
            registry.register("custom", CustomRuleValidator())
            return registry
        }
    }
}
