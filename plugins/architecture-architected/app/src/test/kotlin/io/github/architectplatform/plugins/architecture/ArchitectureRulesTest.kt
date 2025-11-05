package io.github.architectplatform.plugins.architecture

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ArchitectureRulesTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test validation with no rules returns empty result`() {
        val context = ArchitectureContext(enabled = true)
        val rules = ArchitectureRules(context)
        
        val result = rules.validate(tempDir)
        
        assertEquals(0, result.violations.size)
        assertEquals(0, result.totalRulesChecked)
        assertFalse(result.hasErrors)
        assertFalse(result.hasWarnings)
    }

    @Test
    fun `test validation when disabled returns empty result`() {
        val context = ArchitectureContext(enabled = false)
        val rules = ArchitectureRules(context)
        
        val result = rules.validate(tempDir)
        
        assertEquals(0, result.violations.size)
        assertEquals(0, result.totalRulesChecked)
    }

    @Test
    fun `test naming rule validates file names`() {
        // Create a test file
        val srcDir = tempDir.resolve("src/main/kotlin")
        Files.createDirectories(srcDir)
        val testFile = srcDir.resolve("MyTest.kt")
        Files.writeString(testFile, "class MyTest {}")
        
        val rule = ArchitectureRule(
            id = "test-naming",
            description = "Tests must end with Test",
            type = "naming",
            pattern = ".*Test",
            paths = listOf("src/main/.*\\.kt"),
            severity = "warning"
        )
        
        val ruleset = RuleSet(
            enabled = true,
            rules = listOf(rule)
        )
        
        val context = ArchitectureContext(
            enabled = true,
            rulesets = mapOf("naming" to ruleset)
        )
        
        val rules = ArchitectureRules(context)
        val result = rules.validate(tempDir)
        
        assertEquals(1, result.totalRulesChecked)
        // The file matches the pattern, so no violation
        assertEquals(0, result.violations.size)
    }

    @Test
    fun `test naming rule detects violations`() {
        // Create a test file that violates the naming rule
        val srcDir = tempDir.resolve("src/main/kotlin")
        Files.createDirectories(srcDir)
        val testFile = srcDir.resolve("MyController.kt")
        Files.writeString(testFile, "class MyController {}")
        
        val rule = ArchitectureRule(
            id = "test-naming",
            description = "Tests must end with Test",
            type = "naming",
            pattern = ".*Test",
            paths = listOf("src/main/.*Controller\\.kt"),
            severity = "error"
        )
        
        val ruleset = RuleSet(
            enabled = true,
            rules = listOf(rule)
        )
        
        val context = ArchitectureContext(
            enabled = true,
            rulesets = mapOf("naming" to ruleset)
        )
        
        val rules = ArchitectureRules(context)
        val result = rules.validate(tempDir)
        
        assertEquals(1, result.totalRulesChecked)
        assertEquals(1, result.violations.size)
        assertTrue(result.hasErrors)
        
        val violation = result.violations[0]
        assertEquals("test-naming", violation.rule.id)
        assertTrue(violation.message.contains("does not match required pattern"))
    }

    @Test
    fun `test structure rule validates required paths`() {
        val rule = ArchitectureRule(
            id = "required-dirs",
            description = "Required directories",
            type = "structure",
            paths = listOf("src/main", "src/test"),
            severity = "error"
        )
        
        val ruleset = RuleSet(
            enabled = true,
            rules = listOf(rule)
        )
        
        val context = ArchitectureContext(
            enabled = true,
            rulesets = mapOf("structure" to ruleset)
        )
        
        val rules = ArchitectureRules(context)
        val result = rules.validate(tempDir)
        
        // Both directories don't exist, so 2 violations
        assertEquals(2, result.violations.size)
        assertTrue(result.hasErrors)
    }

    @Test
    fun `test shouldFail with error violations`() {
        val rule = ArchitectureRule(
            id = "test",
            severity = "error"
        )
        
        val violation = ArchitectureRules.Violation(
            rule = rule,
            file = tempDir,
            message = "Test violation"
        )
        
        val result = ArchitectureRules.ValidationResult(
            violations = listOf(violation),
            totalRulesChecked = 1,
            filesAnalyzed = 1
        )
        
        assertTrue(result.shouldFail(false, "warn"))
        assertTrue(result.shouldFail(true, "warn"))
        assertTrue(result.shouldFail(false, "fail"))
    }

    @Test
    fun `test shouldFail with warning violations`() {
        val rule = ArchitectureRule(
            id = "test",
            severity = "warning"
        )
        
        val violation = ArchitectureRules.Violation(
            rule = rule,
            file = tempDir,
            message = "Test violation"
        )
        
        val result = ArchitectureRules.ValidationResult(
            violations = listOf(violation),
            totalRulesChecked = 1,
            filesAnalyzed = 1
        )
        
        assertFalse(result.shouldFail(false, "warn"))
        assertTrue(result.shouldFail(true, "warn")) // strict mode
        assertTrue(result.shouldFail(false, "fail")) // fail on any violation
    }

    @Test
    fun `test text report formatting`() {
        val context = ArchitectureContext()
        val rules = ArchitectureRules(context)
        
        val result = ArchitectureRules.ValidationResult(
            violations = emptyList(),
            totalRulesChecked = 5,
            filesAnalyzed = 10
        )
        
        val report = rules.formatTextReport(result)
        
        assertTrue(report.contains("Architecture Validation Report"))
        assertTrue(report.contains("Rules checked: 5"))
        assertTrue(report.contains("Files analyzed: 10"))
        assertTrue(report.contains("No violations found"))
    }

    @Test
    fun `test json report formatting`() {
        val context = ArchitectureContext()
        val rules = ArchitectureRules(context)
        
        val result = ArchitectureRules.ValidationResult(
            violations = emptyList(),
            totalRulesChecked = 5,
            filesAnalyzed = 10
        )
        
        val report = rules.formatJsonReport(result)
        
        assertTrue(report.contains("\"rulesChecked\": 5"))
        assertTrue(report.contains("\"filesAnalyzed\": 10"))
        assertTrue(report.contains("\"violationsFound\": 0"))
    }

    @Test
    fun `test disabled rules are not checked`() {
        val rule = ArchitectureRule(
            id = "disabled-rule",
            description = "This rule is disabled",
            type = "naming",
            enabled = false
        )
        
        val ruleset = RuleSet(
            enabled = true,
            rules = listOf(rule)
        )
        
        val context = ArchitectureContext(
            enabled = true,
            rulesets = mapOf("test" to ruleset)
        )
        
        val rules = ArchitectureRules(context)
        val result = rules.validate(tempDir)
        
        // Rule is disabled, so it shouldn't be checked
        assertEquals(0, result.totalRulesChecked)
    }

    @Test
    fun `test disabled rulesets are not checked`() {
        val rule = ArchitectureRule(
            id = "test-rule",
            type = "naming"
        )
        
        val ruleset = RuleSet(
            enabled = false,
            rules = listOf(rule)
        )
        
        val context = ArchitectureContext(
            enabled = true,
            rulesets = mapOf("test" to ruleset)
        )
        
        val rules = ArchitectureRules(context)
        val result = rules.validate(tempDir)
        
        // Ruleset is disabled, so its rules shouldn't be checked
        assertEquals(0, result.totalRulesChecked)
    }
}
