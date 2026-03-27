package io.github.architectplatform.plugins.quality

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer

class QualityPlugin : ArchitectPlugin<QualityContext> {
  override val id = "quality-plugin"
  override val contextKey: String = "quality"
  override val ctxClass: Class<QualityContext> = QualityContext::class.java
  override var context: QualityContext = QualityContext()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "additionalProperties" to false,
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean", "default" to true),
      "tools" to mapOf(
        "type" to "array",
        "items" to mapOf(
          "type" to "object",
          "additionalProperties" to false,
          "properties" to mapOf(
            "name" to mapOf("type" to "string", "enum" to listOf("sonarqube", "codeclimate")),
            "url" to mapOf("type" to "string"),
            "projectKey" to mapOf("type" to "string"),
            "configFile" to mapOf("type" to "string"),
          ),
          "required" to listOf("name"),
        ),
      ),
      "gates" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "coverage" to mapOf("type" to "integer", "default" to 80, "minimum" to 0, "maximum" to 100),
          "duplications" to mapOf("type" to "integer", "default" to 3, "minimum" to 0),
          "bugs" to mapOf("type" to "integer", "default" to 0, "minimum" to 0),
          "vulnerabilities" to mapOf("type" to "integer", "default" to 0, "minimum" to 0),
        ),
      ),
    ),
  )

  override fun register(registry: TaskRegistry) {
    // quality-lint: Run detected linters for the project
    registry.add(QualityTask(
      id = "quality-lint",
      phase = CoreWorkflow.LINT,
      context = context,
      buildCommand = { _, args, workDir ->
        val linter = QualityTask.detectLinter(workDir)
        if (linter != null) {
          if (args.isNotEmpty()) {
            "$linter ${ShellArgumentSanitizer.escapeShellArgs(args)}"
          } else {
            linter
          }
        } else {
          "echo 'No linter detected for this project'"
        }
      },
    ))

    // quality-analyze: Run static analysis tools
    registry.add(QualityTask(
      id = "quality-analyze",
      phase = CoreWorkflow.VERIFY,
      context = context,
      buildCommand = { ctx, args, _ ->
        val analyzer = QualityTask.detectAnalyzer(ctx)
        if (analyzer != null) {
          if (args.isNotEmpty()) {
            "$analyzer ${ShellArgumentSanitizer.escapeShellArgs(args)}"
          } else {
            analyzer
          }
        } else {
          "echo 'No static analysis tool configured'"
        }
      },
    ))

    // quality-report: Generate quality report
    registry.add(QualityTask(
      id = "quality-report",
      phase = CoreWorkflow.TEST,
      context = context,
      buildCommand = { ctx, _, workDir ->
        val parts = mutableListOf<String>()
        parts.add("echo ${ShellArgumentSanitizer.escapeShellArg("Quality Report")}")
        parts.add("echo ${ShellArgumentSanitizer.escapeShellArg("=============")}")
        parts.add(
          "echo ${ShellArgumentSanitizer.escapeShellArg("Gates: coverage>=${ctx.gates.coverage}%, duplications<=${ctx.gates.duplications}%, bugs<=${ctx.gates.bugs}, vulnerabilities<=${ctx.gates.vulnerabilities}")}",
        )
        val linter = QualityTask.detectLinter(workDir)
        if (linter != null) {
          parts.add("echo ${ShellArgumentSanitizer.escapeShellArg("Detected linter: $linter")}")
        }
        val analyzer = QualityTask.detectAnalyzer(ctx)
        if (analyzer != null) {
          parts.add("echo ${ShellArgumentSanitizer.escapeShellArg("Configured analyzer: $analyzer")}")
        }
        if (ctx.tools.isNotEmpty()) {
          parts.add(
            "echo ${ShellArgumentSanitizer.escapeShellArg("Configured tools: ${ctx.tools.joinToString(", ") { it.name }}")}",
          )
        }
        parts.joinToString(" && ")
      },
    ))

    // quality-gate: Enforce quality gates
    registry.add(QualityTask(
      id = "quality-gate",
      phase = CoreWorkflow.VERIFY,
      context = context,
      buildCommand = { ctx, _, _ ->
        val (passed, message) = QualityTask.checkGates(ctx.gates)
        if (passed) {
          "echo ${ShellArgumentSanitizer.escapeShellArg("Quality gate check: $message")}"
        } else {
          // Use a failing command so the executor throws, triggering TaskResult.failure
          "echo ${ShellArgumentSanitizer.escapeShellArg("Quality gate FAILED: $message")} && exit 1"
        }
      },
    ))
  }
}
