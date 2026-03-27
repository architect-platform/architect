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
    fun `test dependency rule detects forbidden imports with line numbers and suggestion`() {
        val srcDir = tempDir.resolve("src/main/kotlin")
        Files.createDirectories(srcDir)
        Files.writeString(
            srcDir.resolve("UserController.kt"),
            """
                import com.example.UserRepository

                class UserController
            """.trimIndent()
        )

        val rule = ArchitectureRule(
            id = "no-repositories-in-controller",
            description = "Controllers should use services instead of repositories",
            type = "dependency",
            pattern = ".*Controller.*",
            forbidden = listOf(".*Repository.*"),
            suggestion = "Inject a service instead of importing a repository directly.",
        )
        val result = ArchitectureRules(ArchitectureContext(rulesets = mapOf("layered" to RuleSet(rules = listOf(rule))))).validate(tempDir)

        assertEquals(1, result.violations.size)
        assertEquals(1, result.violations.first().line)
        assertEquals("Inject a service instead of importing a repository directly.", result.violations.first().suggestion)
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
    fun `test structure config validates required and forbidden paths`() {
        Files.createDirectories(tempDir.resolve("src/main/kotlin"))
        Files.writeString(tempDir.resolve(".env"), "SECRET=test")

        val context = ArchitectureContext(
            enabled = true,
            structure = ArchitectureStructure(
                required = listOf("src/main/kotlin", "docs"),
                forbidden = listOf(".env"),
            ),
        )

        val result = ArchitectureRules(context).validate(tempDir, setOf("structure"))

        assertEquals(2, result.totalRulesChecked)
        assertEquals(2, result.violations.size)
        assertTrue(result.violations.any { it.message.contains("Required path does not exist: docs") })
        assertTrue(result.violations.any { it.message.contains("Forbidden path detected: .env") })
    }

    @Test
    fun `test import rule detects circular dependencies`() {
        val srcDir = tempDir.resolve("src/main/kotlin/com/example")
        Files.createDirectories(srcDir)
        Files.writeString(
            srcDir.resolve("A.kt"),
            """
                package com.example
                import com.example.B
                class A
            """.trimIndent()
        )
        Files.writeString(
            srcDir.resolve("B.kt"),
            """
                package com.example
                import com.example.A
                class B
            """.trimIndent()
        )

        val rule = ArchitectureRule(
            id = "no-cycles",
            type = "import",
            suggestion = "Extract a shared abstraction.",
        )
        val result = ArchitectureRules(ArchitectureContext(customRules = listOf(rule))).validate(tempDir)

        assertEquals(2, result.violations.size)
        assertTrue(result.violations.all { it.message.contains("Circular import detected") })
    }

    @Test
    fun `test import rule enforces module boundaries`() {
        val apiDir = tempDir.resolve("api/src/main/kotlin/com/example/api")
        Files.createDirectories(apiDir)
        Files.writeString(
            apiDir.resolve("ApiController.kt"),
            """
                package com.example.api
                import com.example.engine.EngineService
                class ApiController
            """.trimIndent()
        )
        val engineDir = tempDir.resolve("engine/src/main/kotlin/com/example/engine")
        Files.createDirectories(engineDir)
        Files.writeString(
            engineDir.resolve("EngineService.kt"),
            """
                package com.example.engine
                class EngineService
            """.trimIndent()
        )

        val rule = ArchitectureRule(
            id = "module-boundaries",
            type = "import",
            moduleBoundaries = mapOf("api" to listOf("core")),
        )
        val result = ArchitectureRules(ArchitectureContext(customRules = listOf(rule))).validate(tempDir)

        assertEquals(1, result.violations.size)
        assertTrue(result.violations.first().message.contains("Cross-module import violation"))
    }

    @Test
    fun `test convention rule detects missing kdoc on public declarations`() {
        val srcDir = tempDir.resolve("src/main/kotlin")
        Files.createDirectories(srcDir)
        Files.writeString(
            srcDir.resolve("Greeter.kt"),
            """
                class Greeter {
                    fun greet(): String = "hello"
                }
            """.trimIndent()
        )

        val rule = ArchitectureRule(
            id = "public-kdoc",
            type = "convention",
            convention = "kdoc-required",
            paths = listOf("src/main/.*\\.kt"),
        )
        val result = ArchitectureRules(ArchitectureContext(customRules = listOf(rule))).validate(tempDir)

        assertEquals(2, result.violations.size)
        assertTrue(result.violations.all { it.message.contains("missing KDoc") })
    }

    @Test
    fun `test convention rule validates matching test class exists`() {
        val srcDir = tempDir.resolve("src/main/kotlin")
        val testDir = tempDir.resolve("src/test/kotlin")
        Files.createDirectories(srcDir)
        Files.createDirectories(testDir)
        Files.writeString(srcDir.resolve("OrderService.kt"), "class OrderService")
        Files.writeString(testDir.resolve("OrderServiceTest.kt"), "class OrderServiceTest")

        val rule = ArchitectureRule(
            id = "tests-required",
            type = "convention",
            convention = "test-class-exists",
            paths = listOf("src/main/.*\\.kt"),
        )
        val result = ArchitectureRules(ArchitectureContext(customRules = listOf(rule))).validate(tempDir)

        assertTrue(result.violations.isEmpty())
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
    fun `test built in preset rulesets contribute rules`() {
        val context = ArchitectureContext(presetRulesets = listOf("clean-architecture"))
        val result = ArchitectureRules(context).validate(tempDir)

        assertTrue(result.totalRulesChecked > 0)
    }

    @Test
    fun `test report includes suggestion field`() {
        val rule = ArchitectureRule(
            id = "suggested",
            type = "structure",
            paths = listOf("src/main"),
            suggestion = "Create src/main before running validation.",
        )
        val result = ArchitectureRules(ArchitectureContext(customRules = listOf(rule))).validate(tempDir)
        val report = ArchitectureRules(ArchitectureContext()).formatTextReport(result)
        val json = ArchitectureRules(ArchitectureContext()).formatJsonReport(result)

        assertTrue(report.contains("Suggestion: Create src/main before running validation."))
        assertTrue(json.contains("\"suggestion\": \"Create src/main before running validation.\""))
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
