package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.engine.EngineHealthChecker

/**
 * Handles `architect doctor` — a diagnostic command that checks the health
 * of the developer environment and Architect project configuration.
 *
 * Checks performed:
 * 1. architect.yml exists in the current directory
 * 2. architect.yml is valid YAML (parseable)
 * 3. Project name is configured in architect.yml
 * 4. Java/JVM is available and version >= 17
 * 5. Git is available
 * 6. Architect Engine is running
 * 7. Gradle is available (optional)
 *
 * Supports `--fix` flag that attempts auto-remediation where possible,
 * and `--plain` flag for CI-friendly output.
 */
class DoctorCommandHandler(
    private val engineHealthChecker: EngineHealthChecker,
) {

    data class DiagnosticCheck(
        val name: String,
        val passed: Boolean,
        val detail: String,
        val remediation: String? = null,
    )

    var plain: Boolean = false

    fun handle(args: List<String>) {
        val fix = args.contains("--fix")
        val localPlain = plain || args.contains("--plain")
        val checks = mutableListOf<DiagnosticCheck>()

        println()
        println(if (localPlain) "=== Architect Doctor ===" else "🩺 Architect Doctor")
        println()

        // Check 1: architect.yml exists
        val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
        val configExists = configFile.exists()
        checks.add(DiagnosticCheck(
            name = "Configuration file (architect.yml)",
            passed = configExists,
            detail = if (configExists) "Found at ${configFile.absolutePath}" else "Not found in current directory",
            remediation = if (!configExists) "Run 'architect init' to create a configuration file" else null,
        ))

        // Check 2: architect.yml is valid YAML
        if (configExists) {
            val validYaml = try {
                val yaml = org.yaml.snakeyaml.Yaml()
                yaml.load<Map<String, Any>>(configFile.inputStream())
                true
            } catch (_: Exception) {
                false
            }
            checks.add(DiagnosticCheck(
                name = "Configuration syntax",
                passed = validYaml,
                detail = if (validYaml) "architect.yml is valid YAML" else "architect.yml has syntax errors",
                remediation = if (!validYaml) "Check architect.yml for YAML syntax errors" else null,
            ))

            // Check 3: Project name configured
            if (validYaml) {
                val yaml = org.yaml.snakeyaml.Yaml()
                @Suppress("UNCHECKED_CAST")
                val config = yaml.load<Map<String, Any>>(configFile.inputStream())
                @Suppress("UNCHECKED_CAST")
                val project = config?.get("project") as? Map<String, Any>
                val hasName = project?.get("name") != null
                checks.add(DiagnosticCheck(
                    name = "Project name configured",
                    passed = hasName,
                    detail = if (hasName) "Project: ${project?.get("name")}" else "No project name defined",
                    remediation = if (!hasName) "Add 'project: { name: your-project }' to architect.yml" else null,
                ))
            }
        }

        // Check 4: Java/JVM version
        val javaVersion = System.getProperty("java.version") ?: "unknown"
        val javaMajor = try {
            javaVersion.split(".").first().toInt()
        } catch (_: Exception) {
            0
        }
        val javaOk = javaMajor >= 17
        checks.add(DiagnosticCheck(
            name = "Java Runtime (>= 17)",
            passed = javaOk,
            detail = "Java $javaVersion (major: $javaMajor)",
            remediation = if (!javaOk) "Install Java 17 or later: https://adoptium.net/" else null,
        ))

        // Check 5: Git available
        val gitAvailable = try {
            val process = ProcessBuilder("git", "--version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor() == 0 && output.contains("git version")
        } catch (_: Exception) {
            false
        }
        val gitVersion = if (gitAvailable) {
            try {
                ProcessBuilder("git", "--version").redirectErrorStream(true).start()
                    .inputStream.bufferedReader().readText().trim()
            } catch (_: Exception) {
                "unknown"
            }
        } else {
            "not found"
        }
        checks.add(DiagnosticCheck(
            name = "Git",
            passed = gitAvailable,
            detail = gitVersion,
            remediation = if (!gitAvailable) "Install Git: https://git-scm.com/downloads" else null,
        ))

        // Check 6: Engine running
        val engineRunning = engineHealthChecker.isRunning()
        checks.add(DiagnosticCheck(
            name = "Architect Engine",
            passed = engineRunning,
            detail = if (engineRunning) "Engine is running and responsive" else "Engine is not running",
            remediation = if (!engineRunning) "Start with 'architect engine start' or use '--embedded' mode" else null,
        ))

        // Check 7: Gradle available (optional)
        val gradleAvailable = try {
            val gradleWrapper = java.io.File(System.getProperty("user.dir"), "gradlew")
            if (gradleWrapper.exists()) {
                true
            } else {
                val process = ProcessBuilder("gradle", "--version").redirectErrorStream(true).start()
                process.waitFor() == 0
            }
        } catch (_: Exception) {
            false
        }
        checks.add(DiagnosticCheck(
            name = "Gradle (optional)",
            passed = gradleAvailable,
            detail = if (gradleAvailable) "Gradle wrapper or system Gradle available" else "Not found (only needed for Gradle projects)",
            remediation = null, // Optional, no remediation needed
        ))

        // Print results
        val passedCount = checks.count { it.passed }
        val failedCount = checks.count { !it.passed }

        for (check in checks) {
            val prefix = if (localPlain) {
                if (check.passed) "[PASS]" else "[FAIL]"
            } else {
                if (check.passed) "✅" else "❌"
            }
            println("  $prefix ${check.name}")
            println("      ${check.detail}")
            if (!check.passed && check.remediation != null) {
                val hint = if (localPlain) "    FIX:" else "    💡"
                println("  $hint ${check.remediation}")
            }
        }

        println()
        val summary = "$passedCount passed, $failedCount failed"
        if (failedCount == 0) {
            println(if (localPlain) "All checks passed! ($summary)" else "🎉 All checks passed! ($summary)")
        } else {
            println(if (localPlain) "Some checks failed ($summary)" else "⚠️  Some checks failed ($summary)")
            if (fix) {
                println()
                println(if (localPlain) "Attempting auto-fix..." else "🔧 Attempting auto-fix...")
                // Auto-fix: create architect.yml if missing
                if (!configExists) {
                    println("  Creating default architect.yml...")
                    val projectName = java.io.File(System.getProperty("user.dir")).name
                    val defaultConfig = """
                        |project:
                        |  name: $projectName
                        |  description: ""
                        |
                        |plugins: []
                    """.trimMargin()
                    configFile.writeText(defaultConfig)
                    println(if (localPlain) "  [PASS] Created architect.yml" else "  ✅ Created architect.yml")
                }
            }
        }
        println()
    }
}
