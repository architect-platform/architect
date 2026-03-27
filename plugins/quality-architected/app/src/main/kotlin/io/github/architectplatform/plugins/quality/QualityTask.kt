package io.github.architectplatform.plugins.quality

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer
import java.io.IOException
import java.nio.file.Files

class QualityTask(
  override val id: String,
  private val phase: Phase,
  private val context: QualityContext,
  private val buildCommand: (QualityContext, List<String>, String) -> String,
) : Task {
  override fun phase(): Phase = phase

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.skipped("Quality checks disabled")
    }
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val workDir = projectContext.dir.toString()
    val command = buildCommand(context, args, workDir)
    return try {
      commandExecutor.execute(command, workingDir = workDir)
      TaskResult.success("Quality task $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure("Quality task $id failed: ${e.message ?: "Unknown error"}")
    }
  }

  companion object {

    /**
     * Detect which linting tool to use based on project files present in the working directory.
     */
    fun detectLinter(workDir: String): String? {
      val dir = java.nio.file.Path.of(workDir)

      // Kotlin/JVM: detekt
      if (Files.exists(dir.resolve("build.gradle.kts")) && Files.exists(dir.resolve("detekt.yml"))) {
        return "./gradlew detekt"
      }

      // JavaScript/TypeScript: ESLint
      if (Files.exists(dir.resolve("package.json"))) {
        val hasEslintConfig = Files.exists(dir.resolve(".eslintrc")) ||
          Files.exists(dir.resolve(".eslintrc.js")) ||
          Files.exists(dir.resolve(".eslintrc.json")) ||
          Files.exists(dir.resolve(".eslintrc.yml")) ||
          Files.exists(dir.resolve("eslint.config.js")) ||
          Files.exists(dir.resolve("eslint.config.mjs")) ||
          Files.exists(dir.resolve("eslint.config.cjs"))
        if (hasEslintConfig) {
          return "npx eslint ."
        }
      }

      // Python: Ruff
      if (Files.exists(dir.resolve("pyproject.toml"))) {
        try {
          val content = Files.readString(dir.resolve("pyproject.toml"))
          if (content.contains("[tool.ruff]")) {
            return "ruff check ."
          }
        } catch (_: IOException) {
          // Ignore read errors from pyproject inspection and continue detection.
        }
      }

      // Rust: Clippy
      if (Files.exists(dir.resolve("Cargo.toml"))) {
        return "cargo clippy"
      }

      return null
    }

    /**
     * Detect which static analysis tool to use based on configured tools and project files.
     */
    fun detectAnalyzer(context: QualityContext): String? {
      // Check configured tools first
      val sonarTool = context.tools.find { it.name.equals("sonarqube", ignoreCase = true) }
      if (sonarTool != null) {
        return buildString {
          append("sonar-scanner")
          sonarTool.url?.let { append(" -Dsonar.host.url=${ShellArgumentSanitizer.escapeShellArg(it)}") }
          sonarTool.projectKey?.let { append(" -Dsonar.projectKey=${ShellArgumentSanitizer.escapeShellArg(it)}") }
        }
      }

      val codeClimateTool = context.tools.find { it.name.equals("codeclimate", ignoreCase = true) }
      if (codeClimateTool != null) {
        return "codeclimate analyze"
      }

      return null
    }

    /**
     * Build quality gate enforcement command output.
     * Returns a pair of (passed, message).
     */
    fun checkGates(gates: QualityGates): Pair<Boolean, String> {
      val messages = mutableListOf<String>()
      var passed = true

      if (gates.coverage < 0 || gates.coverage > 100) {
        messages.add("Invalid coverage threshold: ${gates.coverage}%")
        passed = false
      }
      if (gates.duplications < 0) {
        messages.add("Invalid duplications threshold: ${gates.duplications}%")
        passed = false
      }
      if (gates.bugs < 0) {
        messages.add("Invalid bugs threshold: ${gates.bugs}")
        passed = false
      }
      if (gates.vulnerabilities < 0) {
        messages.add("Invalid vulnerabilities threshold: ${gates.vulnerabilities}")
        passed = false
      }

      if (passed) {
        messages.add("Quality gates configured: coverage>=${gates.coverage}%, duplications<=${gates.duplications}%, bugs<=${gates.bugs}, vulnerabilities<=${gates.vulnerabilities}")
      }

      return Pair(passed, messages.joinToString("; "))
    }
  }
}
