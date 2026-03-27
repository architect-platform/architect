package io.github.architectplatform.plugins.architecture

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArchitectureContextTest {

    @Test
    fun `test default context values`() {
        val context = ArchitectureContext()
        
        assertTrue(context.enabled)
        assertTrue(context.presetRulesets.isEmpty())
        assertTrue(context.rulesets.isEmpty())
        assertTrue(context.customRules.isEmpty())
        assertEquals("warn", context.onViolation)
        assertEquals("text", context.reportFormat)
        assertFalse(context.strict)
    }

    @Test
    fun `test context with custom values`() {
        val rule = ArchitectureRule(
            id = "test-rule",
            description = "Test rule",
            type = "dependency",
            pattern = ".*Test.*"
        )
        
        val ruleset = RuleSet(
            enabled = true,
            description = "Test ruleset",
            rules = listOf(rule)
        )
        
        val context = ArchitectureContext(
            enabled = true,
            presetRulesets = listOf("layered-architecture"),
            rulesets = mapOf("test" to ruleset),
            customRules = listOf(rule),
            onViolation = "fail",
            reportFormat = "json",
            strict = true
        )
        
        assertTrue(context.enabled)
        assertEquals(listOf("layered-architecture"), context.presetRulesets)
        assertEquals(1, context.rulesets.size)
        assertEquals(1, context.customRules.size)
        assertEquals("fail", context.onViolation)
        assertEquals("json", context.reportFormat)
        assertTrue(context.strict)
    }

    @Test
    fun `test ruleset with rules`() {
        val rule1 = ArchitectureRule(
            id = "rule1",
            description = "First rule",
            type = "dependency"
        )
        
        val rule2 = ArchitectureRule(
            id = "rule2",
            description = "Second rule",
            type = "naming"
        )
        
        val ruleset = RuleSet(
            enabled = true,
            description = "Test ruleset",
            rules = listOf(rule1, rule2)
        )
        
        assertTrue(ruleset.enabled)
        assertEquals("Test ruleset", ruleset.description)
        assertEquals(2, ruleset.rules.size)
    }

    @Test
    fun `test architecture rule properties`() {
        val rule = ArchitectureRule(
            id = "test-rule",
            description = "Test rule description",
            type = "dependency",
            pattern = ".*Controller.*",
            paths = listOf("src/main/**"),
            forbidden = listOf(".*Repository.*"),
            required = listOf(".*Service.*"),
            convention = "kdoc-required",
            threshold = 80,
            allowedCycles = listOf("shared/*"),
            moduleBoundaries = mapOf("api" to listOf("core")),
            suggestion = "Use the service layer",
            severity = "error",
            enabled = true
        )
        
        assertEquals("test-rule", rule.id)
        assertEquals("Test rule description", rule.description)
        assertEquals("dependency", rule.type)
        assertEquals(".*Controller.*", rule.pattern)
        assertEquals(1, rule.paths.size)
        assertEquals(1, rule.forbidden.size)
        assertEquals(1, rule.required.size)
        assertEquals("kdoc-required", rule.convention)
        assertEquals(80, rule.threshold)
        assertEquals(listOf("shared/*"), rule.allowedCycles)
        assertEquals(mapOf("api" to listOf("core")), rule.moduleBoundaries)
        assertEquals("Use the service layer", rule.suggestion)
        assertEquals("error", rule.severity)
        assertTrue(rule.enabled)
    }

    @Test
    fun `test rule default values`() {
        val rule = ArchitectureRule(id = "minimal-rule")
        
        assertEquals("minimal-rule", rule.id)
        assertEquals("", rule.description)
        assertEquals("dependency", rule.type)
        assertEquals(".*", rule.pattern)
        assertTrue(rule.paths.isEmpty())
        assertTrue(rule.forbidden.isEmpty())
        assertTrue(rule.required.isEmpty())
        assertNull(rule.validator)
        assertNull(rule.convention)
        assertNull(rule.threshold)
        assertTrue(rule.allowedCycles.isEmpty())
        assertTrue(rule.moduleBoundaries.isEmpty())
        assertNull(rule.suggestion)
        assertEquals("error", rule.severity)
        assertTrue(rule.enabled)
    }

    @Test
    fun `resolvedRulesets includes preset and built in ruleset aliases`() {
        val context = ArchitectureContext(
            presetRulesets = listOf("layered-architecture"),
            rulesets = mapOf("clean-architecture" to RuleSet(enabled = true))
        )

        val resolved = context.resolvedRulesets()

        assertTrue(resolved.containsKey("layered-architecture"))
        assertTrue(resolved.containsKey("clean-architecture"))
        assertTrue(resolved.getValue("clean-architecture").rules.isNotEmpty())
    }
}
