package io.github.architectplatform.plugins.architecture

import io.github.architectplatform.api.testing.ArchitectPluginContract
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArchitecturePluginTest {

    @Test
    fun `plugin satisfies shared architect plugin contract`() {
        ArchitectPluginContract { ArchitecturePlugin() }
            .verify(
                ArchitectPluginContract.Verification(
                    config = mapOf(
                        "enabled" to false,
                        "presetRulesets" to emptyList<String>(),
                        "rulesets" to emptyMap<String, Any>(),
                        "customRules" to emptyList<Any>(),
                        "structure" to mapOf(
                            "enabled" to true,
                            "required" to emptyList<String>(),
                            "forbidden" to emptyList<String>(),
                        ),
                        "boundaries" to emptyMap<String, Any>(),
                        "onViolation" to "warn",
                        "reportFormat" to "text",
                        "strict" to false,
                    ),
                    expectedTaskIds = setOf("architecture-validate"),
                    executableTaskIds = listOf("architecture-validate"),
                )
            )
    }

    @Test
    fun `test plugin id and context key`() {
        val plugin = ArchitecturePlugin()
        
        assertEquals("architecture-plugin", plugin.id)
        assertEquals("architecture", plugin.contextKey)
        assertEquals(ArchitectureContext::class.java, plugin.ctxClass)
    }

    @Test
    fun `test plugin context initialization`() {
        val plugin = ArchitecturePlugin()
        
        assertNotNull(plugin.context)
        assertTrue(plugin.context.enabled)
    }

    @Test
    fun `test plugin context can be set`() {
        val plugin = ArchitecturePlugin()
        val customContext = ArchitectureContext(
            enabled = false,
            onViolation = "fail"
        )
        
        plugin.context = customContext
        
        assertFalse(plugin.context.enabled)
        assertEquals("fail", plugin.context.onViolation)
    }

    @Test
    fun `test plugin exposes config schema for preset rulesets and rule types`() {
        val schema = ArchitecturePlugin().configSchema()

        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Any>
        assertTrue(properties.containsKey("presetRulesets"))
        assertTrue(properties.containsKey("structure"))
        assertTrue(properties.containsKey("boundaries"))
        @Suppress("UNCHECKED_CAST")
        val defs = schema["\$defs"] as Map<String, Any>
        assertTrue(defs.containsKey("rule"))
        assertTrue(defs.containsKey("structure"))
    }
}
