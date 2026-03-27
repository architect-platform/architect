package io.github.architectplatform.plugins.architecture

import io.github.architectplatform.api.testing.ArchitectPluginContractTestSuite

class ArchitecturePluginContractTest : ArchitectPluginContractTestSuite<ArchitectureContext>() {

    override fun createPlugin() = ArchitecturePlugin()

    override fun expectedTaskIds() = setOf("architecture-validate")

    override fun executableTaskIds() = listOf("architecture-validate")

    override fun pluginConfig(): Any = mapOf(
        "enabled" to false,
        "presetRulesets" to emptyList<String>(),
        "rulesets" to emptyMap<String, Any>(),
        "customRules" to emptyList<Any>(),
        "onViolation" to "warn",
        "reportFormat" to "text",
        "strict" to false,
    )
}
