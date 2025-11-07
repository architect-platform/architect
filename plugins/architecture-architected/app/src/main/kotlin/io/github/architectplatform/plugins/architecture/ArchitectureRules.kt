package io.github.architectplatform.plugins.architecture

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.toList

/**
 * Manages architectural rules and validates them against project files.
 *
 * This class provides functionality to:
 * - Load and parse architectural rules from configuration
 * - Validate project structure against defined rules
 * - Generate validation reports
 * - Support custom rule validators
 */
class ArchitectureRules(private val context: ArchitectureContext) {

    /**
     * Represents a violation of an architectural rule.
     *
     * @property rule The rule that was violated
     * @property file The file where the violation occurred
     * @property line The line number where the violation occurred (if applicable)
     * @property message Detailed message about the violation
     */
    data class Violation(
        val rule: ArchitectureRule,
        val file: Path,
        val line: Int? = null,
        val message: String
    )

    /**
     * Result of architectural validation.
     *
     * @property violations List of all violations found
     * @property totalRulesChecked Number of rules that were checked
     * @property filesAnalyzed Number of files analyzed
     */
    data class ValidationResult(
        val violations: List<Violation>,
        val totalRulesChecked: Int,
        val filesAnalyzed: Int
    ) {
        val hasErrors: Boolean
            get() = violations.any { it.rule.severity == "error" }
        
        val hasWarnings: Boolean
            get() = violations.any { it.rule.severity == "warning" }
        
        fun shouldFail(strict: Boolean, onViolation: String): Boolean {
            return hasErrors || (strict && hasWarnings) || (onViolation == "fail" && violations.isNotEmpty())
        }
    }

    /**
     * Validates architectural rules against the project directory.
     *
     * @param projectDir The root directory of the project
     * @return ValidationResult containing all violations found
     */
    fun validate(projectDir: Path): ValidationResult {
        if (!context.enabled) {
            return ValidationResult(emptyList(), 0, 0)
        }

        val violations = mutableListOf<Violation>()
        val allRules = getAllRules()
        val enabledRules = allRules.filter { it.enabled }
        
        // Get all source files from the project
        val sourceFiles = findSourceFiles(projectDir)
        
        // Validate each rule
        for (rule in enabledRules) {
            when (rule.type.lowercase()) {
                "dependency" -> violations.addAll(validateDependencyRule(rule, sourceFiles, projectDir))
                "naming" -> violations.addAll(validateNamingRule(rule, sourceFiles, projectDir))
                "structure" -> violations.addAll(validateStructureRule(rule, projectDir))
                "custom" -> violations.addAll(validateCustomRule(rule, sourceFiles, projectDir))
                else -> {
                    // Unknown rule type, skip
                }
            }
        }
        
        return ValidationResult(violations, enabledRules.size, sourceFiles.size)
    }

    /**
     * Gets all rules from all enabled rulesets plus custom rules.
     *
     * @return List of all architectural rules
     */
    private fun getAllRules(): List<ArchitectureRule> {
        val rules = mutableListOf<ArchitectureRule>()
        
        // Add rules from rulesets
        for ((_, ruleset) in context.rulesets) {
            if (ruleset.enabled) {
                rules.addAll(ruleset.rules)
            }
        }
        
        // Add custom rules
        rules.addAll(context.customRules)
        
        return rules
    }

    /**
     * Finds all source files in the project directory.
     *
     * @param projectDir The root directory of the project
     * @return List of paths to source files
     */
    private fun findSourceFiles(projectDir: Path): List<Path> {
        val sourceExtensions = setOf("kt", "java", "js", "ts", "py", "rb", "go", "rs", "cpp", "c", "h", "hpp")
        
        return try {
            Files.walk(projectDir)
                .filter { it.isRegularFile() }
                .filter { sourceExtensions.contains(it.extension) }
                .filter { !shouldExcludePath(it, projectDir) }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Determines if a path should be excluded from validation.
     *
     * @param path The path to check
     * @param projectDir The project root directory
     * @return True if the path should be excluded
     */
    private fun shouldExcludePath(path: Path, projectDir: Path): Boolean {
        val relativePath = projectDir.relativize(path).toString()
        val excludePatterns = listOf(
            "build/",
            "target/",
            "node_modules/",
            ".git/",
            "dist/",
            "out/",
            ".gradle/",
            "bin/"
        )
        
        return excludePatterns.any { relativePath.contains(it) }
    }

    /**
     * Validates a dependency rule.
     *
     * Checks if files matching the pattern have forbidden or missing required dependencies.
     *
     * @param rule The dependency rule to validate
     * @param sourceFiles List of source files to check
     * @param projectDir The project root directory
     * @return List of violations found
     */
    private fun validateDependencyRule(
        rule: ArchitectureRule,
        sourceFiles: List<Path>,
        projectDir: Path
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        val patternRegex = try {
            Regex(rule.pattern)
        } catch (e: Exception) {
            return listOf(Violation(rule, projectDir, null, "Invalid pattern regex: ${rule.pattern}"))
        }
        
        for (file in sourceFiles) {
            // Check if file matches the pattern
            val content = try {
                file.readText()
            } catch (e: Exception) {
                continue
            }
            
            // Check if file matches path patterns if specified
            if (rule.paths.isNotEmpty()) {
                val relativePath = projectDir.relativize(file).toString()
                val matchesPath = rule.paths.any { pathPattern ->
                    try {
                        Regex(pathPattern).matches(relativePath)
                    } catch (e: Exception) {
                        false
                    }
                }
                if (!matchesPath) {
                    continue
                }
            }
            
            // Extract class/module names from the file
            val classNames = extractClassNames(content, file)
            
            // Check if any class matches the pattern
            val matchingClasses = classNames.filter { patternRegex.matches(it) }
            
            if (matchingClasses.isEmpty()) {
                continue
            }
            
            // Check forbidden dependencies
            for (forbiddenPattern in rule.forbidden) {
                try {
                    val forbiddenRegex = Regex(forbiddenPattern)
                    val imports = extractImports(content)
                    
                    for (import in imports) {
                        if (forbiddenRegex.matches(import)) {
                            violations.add(
                                Violation(
                                    rule,
                                    file,
                                    null,
                                    "File contains forbidden dependency: $import (matched pattern: $forbiddenPattern)"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Invalid regex, skip
                }
            }
            
            // Check required dependencies
            for (requiredPattern in rule.required) {
                try {
                    val requiredRegex = Regex(requiredPattern)
                    val imports = extractImports(content)
                    val hasRequired = imports.any { requiredRegex.matches(it) }
                    
                    if (!hasRequired) {
                        violations.add(
                            Violation(
                                rule,
                                file,
                                null,
                                "File is missing required dependency matching pattern: $requiredPattern"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Invalid regex, skip
                }
            }
        }
        
        return violations
    }

    /**
     * Validates a naming rule.
     *
     * Checks if files or classes follow the specified naming conventions.
     *
     * @param rule The naming rule to validate
     * @param sourceFiles List of source files to check
     * @param projectDir The project root directory
     * @return List of violations found
     */
    private fun validateNamingRule(
        rule: ArchitectureRule,
        sourceFiles: List<Path>,
        projectDir: Path
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        val patternRegex = try {
            Regex(rule.pattern)
        } catch (e: Exception) {
            return listOf(Violation(rule, projectDir, null, "Invalid pattern regex: ${rule.pattern}"))
        }
        
        for (file in sourceFiles) {
            // Check if file matches path patterns if specified
            if (rule.paths.isNotEmpty()) {
                val relativePath = projectDir.relativize(file).toString()
                val matchesPath = rule.paths.any { pathPattern ->
                    try {
                        Regex(pathPattern).matches(relativePath)
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (matchesPath) {
                    // Path matches, now check if naming pattern is followed
                    val fileName = file.fileName.toString()
                    val fileNameWithoutExt = fileName.substringBeforeLast('.')
                    
                    if (!patternRegex.matches(fileNameWithoutExt)) {
                        violations.add(
                            Violation(
                                rule,
                                file,
                                null,
                                "File name '$fileNameWithoutExt' does not match required pattern: ${rule.pattern}"
                            )
                        )
                    }
                }
            }
        }
        
        return violations
    }

    /**
     * Validates a structure rule.
     *
     * Checks if the project structure follows the specified conventions.
     *
     * @param rule The structure rule to validate
     * @param projectDir The project root directory
     * @return List of violations found
     */
    private fun validateStructureRule(
        rule: ArchitectureRule,
        projectDir: Path
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        
        // Structure rules check for existence of directories or files
        if (rule.paths.isNotEmpty()) {
            for (pathPattern in rule.paths) {
                val path = projectDir.resolve(pathPattern)
                if (!Files.exists(path)) {
                    violations.add(
                        Violation(
                            rule,
                            projectDir,
                            null,
                            "Required path does not exist: $pathPattern"
                        )
                    )
                }
            }
        }
        
        return violations
    }

    /**
     * Validates a custom rule.
     *
     * Delegates to a custom validator if specified.
     * Note: Custom rule validation requires implementing a custom validator class
     * and is currently not supported. Users should use built-in rule types instead.
     *
     * @param rule The custom rule to validate
     * @param sourceFiles List of source files to check
     * @param projectDir The project root directory
     * @return List of violations found (currently always empty as custom validators are not yet implemented)
     */
    private fun validateCustomRule(
        rule: ArchitectureRule,
        sourceFiles: List<Path>,
        projectDir: Path
    ): List<Violation> {
        // Custom rules require a validator class implementation
        // This functionality is planned for future releases
        // For now, return a warning that custom rules are not yet supported
        if (rule.validator != null) {
            return listOf(
                Violation(
                    rule = rule,
                    file = projectDir,
                    message = "Custom rule validators are not yet implemented. Rule '${rule.id}' will be skipped."
                )
            )
        }
        return emptyList()
    }

    /**
     * Extracts class/module names from source code.
     *
     * @param content The source code content
     * @param file The file being analyzed
     * @return List of class/module names found
     */
    private fun extractClassNames(content: String, file: Path): List<String> {
        val names = mutableListOf<String>()
        
        // Simple regex patterns for different languages
        val patterns = listOf(
            Regex("""class\s+(\w+)"""),          // Java, Kotlin, C++
            Regex("""interface\s+(\w+)"""),      // Java, Kotlin, TypeScript
            Regex("""object\s+(\w+)"""),         // Kotlin
            Regex("""data class\s+(\w+)"""),     // Kotlin
            Regex("""enum class\s+(\w+)"""),     // Kotlin
            Regex("""function\s+(\w+)"""),       // JavaScript, TypeScript
            Regex("""def\s+(\w+)"""),            // Python
            Regex("""module\s+(\w+)"""),         // Ruby, Rust
            Regex("""struct\s+(\w+)"""),         // Go, C, Rust
            Regex("""trait\s+(\w+)"""),          // Rust
        )
        
        for (pattern in patterns) {
            names.addAll(pattern.findAll(content).map { it.groupValues[1] }.toList())
        }
        
        return names.distinct()
    }

    /**
     * Extracts import statements from source code.
     *
     * @param content The source code content
     * @return List of imported packages/modules
     */
    private fun extractImports(content: String): List<String> {
        val imports = mutableListOf<String>()
        
        // Regex patterns for different languages
        val patterns = listOf(
            Regex("""import\s+([a-zA-Z0-9_.]+)"""),       // Java, Kotlin, Python
            Regex("""from\s+([a-zA-Z0-9_.]+)\s+import"""), // Python
            Regex("""require\(['"]([^'"]+)['"]\)"""),      // Node.js
            Regex("""import\s+.*\s+from\s+['"]([^'"]+)['"]"""), // ES6
            Regex("""use\s+([a-zA-Z0-9_\\:]+)"""),        // Rust
        )
        
        for (pattern in patterns) {
            imports.addAll(pattern.findAll(content).map { it.groupValues[1] }.toList())
        }
        
        return imports.distinct()
    }

    /**
     * Formats the validation result as a text report.
     *
     * @param result The validation result
     * @return Formatted text report
     */
    fun formatTextReport(result: ValidationResult): String {
        val report = StringBuilder()
        
        report.appendLine("=" .repeat(80))
        report.appendLine("Architecture Validation Report")
        report.appendLine("=" .repeat(80))
        report.appendLine()
        report.appendLine("Summary:")
        report.appendLine("  Rules checked: ${result.totalRulesChecked}")
        report.appendLine("  Files analyzed: ${result.filesAnalyzed}")
        report.appendLine("  Violations found: ${result.violations.size}")
        report.appendLine()
        
        if (result.violations.isEmpty()) {
            report.appendLine("✓ No violations found. All architectural rules are satisfied.")
        } else {
            report.appendLine("Violations:")
            report.appendLine()
            
            val groupedByRule = result.violations.groupBy { it.rule.id }
            
            for ((ruleId, violations) in groupedByRule) {
                val rule = violations.first().rule
                report.appendLine("Rule: $ruleId [${rule.severity.uppercase()}]")
                report.appendLine("  Description: ${rule.description}")
                report.appendLine("  Violations: ${violations.size}")
                report.appendLine()
                
                for (violation in violations) {
                    report.appendLine("    • ${violation.file.fileName}")
                    report.appendLine("      ${violation.message}")
                }
                report.appendLine()
            }
        }
        
        report.appendLine("=" .repeat(80))
        
        return report.toString()
    }

    /**
     * Escapes a string for safe inclusion in JSON.
     * Handles quotes, backslashes, and control characters.
     *
     * @param str The string to escape
     * @return JSON-safe escaped string
     */
    private fun escapeJsonString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Formats the validation result as a JSON report.
     *
     * @param result The validation result
     * @return JSON formatted report
     */
    fun formatJsonReport(result: ValidationResult): String {
        val violations = result.violations.map { v ->
            mapOf(
                "rule" to escapeJsonString(v.rule.id),
                "severity" to escapeJsonString(v.rule.severity),
                "file" to escapeJsonString(v.file.toString()),
                "line" to v.line,
                "message" to escapeJsonString(v.message)
            )
        }
        
        // Simple JSON formatting with proper escaping
        // Note: In production, consider using a JSON library like Jackson or kotlinx.serialization
        val json = StringBuilder()
        json.appendLine("{")
        json.appendLine("  \"summary\": {")
        json.appendLine("    \"rulesChecked\": ${result.totalRulesChecked},")
        json.appendLine("    \"filesAnalyzed\": ${result.filesAnalyzed},")
        json.appendLine("    \"violationsFound\": ${result.violations.size},")
        json.appendLine("    \"hasErrors\": ${result.hasErrors},")
        json.appendLine("    \"hasWarnings\": ${result.hasWarnings}")
        json.appendLine("  },")
        json.appendLine("  \"violations\": [")
        
        violations.forEachIndexed { index, v ->
            json.appendLine("    {")
            json.appendLine("      \"rule\": \"${v["rule"]}\",")
            json.appendLine("      \"severity\": \"${v["severity"]}\",")
            json.appendLine("      \"file\": \"${v["file"]}\",")
            json.appendLine("      \"line\": ${v["line"]},")
            json.appendLine("      \"message\": \"${v["message"]}\"")
            json.append("    }")
            if (index < violations.size - 1) {
                json.appendLine(",")
            } else {
                json.appendLine()
            }
        }
        
        json.appendLine("  ]")
        json.appendLine("}")
        
        return json.toString()
    }
}
