package io.github.architectplatform.plugins.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.resolvePath
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer
import java.nio.file.Files
import java.nio.file.Path

class SecurityTask(
  private val operation: Operation,
  private val phase: Phase,
  private val context: SecurityContext,
) : Task {
  override val id: String = operation.taskId

  override fun phase(): Phase = phase

  override fun description(): String = operation.description

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.skipped("Security tasks disabled")
    }

    val workingDir = try {
      projectContext.resolvePath(context.workingDirectory, "Security working directory")
    } catch (e: IllegalArgumentException) {
      return TaskResult.failure(
        "Security task $id failed with invalid working directory: ${e.message}",
      )
    }

    val plan = try {
      buildPlan(projectContext, workingDir, args)
    } catch (e: IllegalArgumentException) {
      return TaskResult.failure("Security task $id failed: ${e.message}")
    }

    plan.skipReason?.let { return TaskResult.skipped(it) }

    val executor = environment.service(CommandExecutor::class.java)
    return if (operation == Operation.SBOM) {
      executeSbom(executor, workingDir, plan)
    } else {
      executeAnalysis(executor, workingDir, plan)
    }
  }

  private fun buildPlan(
    projectContext: ProjectContext,
    workingDir: Path,
    args: List<String>,
  ): ExecutionPlan =
    when (operation) {
      Operation.SCAN -> buildScanPlan(projectContext, args)
      Operation.AUDIT -> buildAuditPlan(workingDir, args)
      Operation.SBOM -> buildSbomPlan(projectContext, args)
    }

  private fun buildScanPlan(
    projectContext: ProjectContext,
    args: List<String>,
  ): ExecutionPlan {
    if (!context.scan.enabled) {
      return ExecutionPlan(skipReason = "Security scanning disabled")
    }

    val requestedTools = validateTools(context.scan.normalizedTools(), SCAN_AND_AUDIT_TOOLS, "security.scan.tools")
    val skippedTools = mutableListOf<String>()
    val threshold = context.scan.threshold()
    val executions = requestedTools.mapNotNull { tool ->
      when (tool.kind) {
        ToolKind.SCAN -> buildScanExecution(tool, projectContext, args, skippedTools)
        ToolKind.AUDIT -> {
          skippedTools += "${tool.id} is executed by security-audit"
          null
        }
        ToolKind.SBOM -> null
      }
    }

    return if (executions.isEmpty()) {
      ExecutionPlan(
        skipReason = "No runnable security scanners are configured",
        skippedTools = skippedTools,
        threshold = threshold,
      )
    } else {
      ExecutionPlan(executions = executions, threshold = threshold, skippedTools = skippedTools)
    }
  }

  private fun buildScanExecution(
    tool: SecurityTool,
    projectContext: ProjectContext,
    args: List<String>,
    skippedTools: MutableList<String>,
  ): ToolExecution? {
    val appendArgs = appendArgs(args)
    return when (tool) {
      SecurityTool.TRIVY -> {
        val thresholdLevels = severityLevelsAtOrAbove(context.scan.threshold())
        val override = context.scan.commands.trivy
        val command = override?.takeIf { it.isNotBlank() }
          ?.let { "$it$appendArgs" }
          ?: "trivy fs --format json --quiet --severity $thresholdLevels --exit-code 0 .$appendArgs"
        ToolExecution(tool = tool, command = command)
      }
      SecurityTool.SNYK -> {
        val override = context.scan.commands.snyk
        val threshold = ShellArgumentSanitizer.requireSafeIdentifier(
          context.scan.threshold().name.lowercase(),
          "security scan severity threshold",
        )
        val command = override?.takeIf { it.isNotBlank() }
          ?.let { "$it$appendArgs" }
          ?: "snyk test --json --severity-threshold=$threshold$appendArgs"
        ToolExecution(tool = tool, command = command)
      }
      SecurityTool.CODEQL -> {
        val override = context.scan.commands.codeql
        if (!override.isNullOrBlank()) {
          val outputPath = resolveRelativePath(projectContext, context.scan.codeqlOutput, "CodeQL SARIF output")
          Files.createDirectories(outputPath.parent)
          return ToolExecution(tool = tool, command = "$override$appendArgs", artifactPath = outputPath)
        }

        val databasePath = context.scan.codeqlDatabase
          ?: run {
            skippedTools += "codeql requires security.scan.codeqlDatabase or security.scan.commands.codeql"
            return null
          }
        val querySuite = context.scan.codeqlQuerySuite
          ?: run {
            skippedTools += "codeql requires security.scan.codeqlQuerySuite or security.scan.commands.codeql"
            return null
          }
        val resolvedDatabase = resolveRelativePath(projectContext, databasePath, "CodeQL database")
        val resolvedOutput = resolveRelativePath(projectContext, context.scan.codeqlOutput, "CodeQL SARIF output")
        Files.createDirectories(resolvedOutput.parent)
        ToolExecution(
          tool = tool,
          command = buildString {
            append("codeql database analyze ")
            append(ShellArgumentSanitizer.escapeShellArg(resolvedDatabase.toString()))
            append(' ')
            append(ShellArgumentSanitizer.escapeShellArg(querySuite))
            append(" --format=sarif-latest --output=")
            append(ShellArgumentSanitizer.escapeShellArg(resolvedOutput.toString()))
            append(appendArgs)
          },
          artifactPath = resolvedOutput,
        )
      }
      else -> null
    }
  }

  private fun buildAuditPlan(
    workingDir: Path,
    args: List<String>,
  ): ExecutionPlan {
    if (!context.audit.enabled) {
      return ExecutionPlan(skipReason = "Security audits disabled")
    }

    val configuredTools = context.audit.normalizedTools()
    val fallbackTools = context.scan.normalizedTools().filter { SecurityTool.fromId(it)?.kind == ToolKind.AUDIT }
    val toolIds = when {
      configuredTools.isNotEmpty() -> configuredTools
      fallbackTools.isNotEmpty() -> fallbackTools
      else -> detectAuditTools(workingDir).map { it.id }
    }
    val threshold = context.audit.threshold()
    val requestedTools = validateTools(toolIds, AUDIT_TOOLS, "security.audit.tools")
    if (requestedTools.isEmpty()) {
      return ExecutionPlan(skipReason = "No supported dependency audit tools were detected", threshold = threshold)
    }

    val executions = requestedTools.map { tool ->
      val override = when (tool) {
        SecurityTool.NPM_AUDIT -> context.audit.commands.npmAudit
        SecurityTool.PIP_AUDIT -> context.audit.commands.pipAudit
        SecurityTool.CARGO_AUDIT -> context.audit.commands.cargoAudit
        else -> null
      }
      val command = override?.takeIf { it.isNotBlank() }
        ?.let { "$it${appendArgs(args)}" }
        ?: defaultAuditCommand(tool, args)
      ToolExecution(tool = tool, command = command)
    }

    return ExecutionPlan(executions = executions, threshold = threshold)
  }

  private fun defaultAuditCommand(
    tool: SecurityTool,
    args: List<String>,
  ): String {
    val extraArgs = appendArgs(args)
    return when (tool) {
      SecurityTool.NPM_AUDIT -> "npm audit --json$extraArgs"
      SecurityTool.PIP_AUDIT -> "pip-audit --format=json$extraArgs"
      SecurityTool.CARGO_AUDIT -> {
        val threshold = ShellArgumentSanitizer.requireSafeIdentifier(
          context.audit.threshold().name.lowercase(),
          "security audit severity threshold",
        )
        "cargo audit --json --severity-threshold $threshold$extraArgs"
      }
      else -> throw IllegalArgumentException("Unsupported audit tool: ${tool.id}")
    }
  }

  private fun buildSbomPlan(
    projectContext: ProjectContext,
    args: List<String>,
  ): ExecutionPlan {
    if (!context.sbom.enabled) {
      return ExecutionPlan(skipReason = "SBOM generation disabled")
    }

    val requestedTool = SecurityTool.fromId(context.sbom.normalizedTool())
      ?: throw IllegalArgumentException(
        "Unsupported security.sbom.tool '${context.sbom.tool}'. Supported tools: trivy.",
      )
    require(requestedTool == SecurityTool.TRIVY) {
      "Unsupported security.sbom.tool '${context.sbom.tool}'. Supported tools: trivy."
    }

    val outputPath = resolveRelativePath(projectContext, context.sbom.output, "Security SBOM output")
    Files.createDirectories(outputPath.parent)
    val format = when (context.sbom.normalizedFormat()) {
      "cyclonedx" -> "cyclonedx"
      "spdx" -> "spdx-json"
      else -> throw IllegalArgumentException(
        "Unsupported security.sbom.format '${context.sbom.format}'. Expected cyclonedx or spdx.",
      )
    }
    val command = context.sbom.command?.takeIf { it.isNotBlank() }
      ?.let { "$it${appendArgs(args)}" }
      ?: buildString {
        append("trivy fs --format ")
        append(ShellArgumentSanitizer.requireSafeIdentifier(format, "SBOM format"))
        append(" --output ")
        append(ShellArgumentSanitizer.escapeShellArg(outputPath.toString()))
        append(" .")
        append(appendArgs(args))
      }

    return ExecutionPlan(
      executions = listOf(ToolExecution(tool = requestedTool, command = command, artifactPath = outputPath)),
    )
  }

  private fun executeAnalysis(
    executor: CommandExecutor,
    workingDir: Path,
    plan: ExecutionPlan,
  ): TaskResult {
    val threshold = requireNotNull(plan.threshold)
    val summaries = mutableListOf<ToolSummary>()
    val failures = mutableListOf<String>()

    for (execution in plan.executions) {
      val result = try {
        executor.executeWithResult(execution.command, workingDir.toString())
      } catch (e: Exception) {
        failures += "${execution.tool.id}: ${e.message ?: "Unknown error"}"
        summaries += ToolSummary(tool = execution.tool, command = execution.command, error = e.message ?: "Unknown error")
        continue
      }

      val summary = summarizeExecution(execution, result, threshold)
      summaries += summary
      if (summary.error != null) {
        failures += "${execution.tool.id}: ${summary.error}"
      }
    }

    val aggregateCounts = aggregateSeverityCounts(summaries)
    val data = mutableMapOf<String, Any>(
      "operation" to operation.taskId,
      "workingDirectory" to workingDir.toString(),
      "threshold" to threshold.name.lowercase(),
      "tools" to plan.executions.map { it.tool.id },
      "severityCounts" to severityCountData(aggregateCounts),
      "issueCount" to aggregateCounts.values.sum(),
      "reports" to summaries.map { it.toData() },
    )
    if (plan.skippedTools.isNotEmpty()) {
      data["skippedTools"] = plan.skippedTools
    }

    val issuesAtOrAboveThreshold = countAtOrAbove(aggregateCounts, threshold)
    if (issuesAtOrAboveThreshold > 0) {
      failures += "found $issuesAtOrAboveThreshold issue(s) at or above ${threshold.name.lowercase()}"
    }

    return if (failures.isEmpty()) {
      TaskResult.success(
        "Security task $id completed with ${aggregateCounts.values.sum()} issue(s) below ${threshold.name.lowercase()} threshold",
        data = data,
      )
    } else {
      TaskResult.failure(
        "Security task $id failed: ${failures.joinToString("; ")}",
        data = data,
      )
    }
  }

  private fun executeSbom(
    executor: CommandExecutor,
    workingDir: Path,
    plan: ExecutionPlan,
  ): TaskResult {
    val execution = plan.executions.single()
    val result = try {
      executor.executeWithResult(execution.command, workingDir.toString())
    } catch (e: Exception) {
      return TaskResult.failure("Security task $id failed: ${e.message ?: "Unknown error"}")
    }

    val data = mapOf(
      "operation" to operation.taskId,
      "workingDirectory" to workingDir.toString(),
      "tool" to execution.tool.id,
      "command" to execution.command,
      "sbomFormat" to context.sbom.normalizedFormat(),
      "sbomOutput" to requireNotNull(execution.artifactPath).toString(),
      "exitCode" to result.exitCode,
    )

    return if (result.success) {
      TaskResult.success(
        "Security SBOM generated at ${requireNotNull(execution.artifactPath).fileName}",
        data = data,
      )
    } else {
      TaskResult.failure(
        "Security task $id failed: ${commandFailureMessage(result)}",
        data = data,
      )
    }
  }

  private fun summarizeExecution(
    execution: ToolExecution,
    result: CommandResult,
    threshold: SecuritySeverity,
  ): ToolSummary {
    val counts = parseSecurityCounts(execution.tool, result, execution.artifactPath)
    val parseError = if (counts.isEmpty() && !result.success) commandFailureMessage(result) else null
    val issuesAtOrAboveThreshold = countAtOrAbove(counts, threshold)
    return ToolSummary(
      tool = execution.tool,
      command = execution.command,
      exitCode = result.exitCode,
      severityCounts = counts,
      error = parseError,
      issuesAtOrAboveThreshold = issuesAtOrAboveThreshold,
    )
  }

  private fun parseSecurityCounts(
    tool: SecurityTool,
    result: CommandResult,
    artifactPath: Path?,
  ): Map<SecuritySeverity, Int> {
    val payload = when {
      artifactPath != null && Files.exists(artifactPath) -> Files.readString(artifactPath)
      result.stdout.isNotBlank() -> result.stdout
      result.stderr.isNotBlank() -> result.stderr
      else -> return emptyMap()
    }

    val root = runCatching { mapper.readTree(payload) }.getOrNull() ?: return emptyMap()
    return when (tool) {
      SecurityTool.TRIVY -> parseTrivy(root)
      SecurityTool.SNYK -> parseSnyk(root)
      SecurityTool.CODEQL -> parseCodeql(root)
      SecurityTool.NPM_AUDIT -> parseNpmAudit(root)
      SecurityTool.PIP_AUDIT -> parsePipAudit(root)
      SecurityTool.CARGO_AUDIT -> parseCargoAudit(root)
    }
  }

  private fun parseTrivy(root: JsonNode): Map<SecuritySeverity, Int> {
    val counts = mutableSeverityCounts()
    root.path("Results").forEach { result ->
      result.path("Vulnerabilities").forEach { vulnerability ->
        incrementSeverity(counts, SecuritySeverity.fromText(vulnerability.path("Severity").asText(null)))
      }
    }
    return counts.filterValues { it > 0 }
  }

  private fun parseSnyk(root: JsonNode): Map<SecuritySeverity, Int> {
    val counts = mutableSeverityCounts()
    val vulnerabilities = if (root.isArray) root else root.path("vulnerabilities")
    vulnerabilities.forEach { vulnerability ->
      incrementSeverity(counts, SecuritySeverity.fromText(vulnerability.path("severity").asText(null)))
    }
    return counts.filterValues { it > 0 }
  }

  private fun parseNpmAudit(root: JsonNode): Map<SecuritySeverity, Int> {
    val metadataCounts = root.path("metadata").path("vulnerabilities")
    if (metadataCounts.isObject) {
      val counts = mutableSeverityCounts()
      incrementSeverity(counts, SecuritySeverity.CRITICAL, metadataCounts.path("critical").asInt(0))
      incrementSeverity(counts, SecuritySeverity.HIGH, metadataCounts.path("high").asInt(0))
      incrementSeverity(counts, SecuritySeverity.MEDIUM, metadataCounts.path("moderate").asInt(0))
      incrementSeverity(counts, SecuritySeverity.LOW, metadataCounts.path("low").asInt(0))
      return counts.filterValues { it > 0 }
    }

    val counts = mutableSeverityCounts()
    root.path("vulnerabilities").properties().forEach { (_, vulnerability) ->
      incrementSeverity(counts, SecuritySeverity.fromText(vulnerability.path("severity").asText(null)))
    }
    return counts.filterValues { it > 0 }
  }

  private fun parsePipAudit(root: JsonNode): Map<SecuritySeverity, Int> {
    val counts = mutableSeverityCounts()
    val dependencies = if (root.isArray) root else root.path("dependencies")
    dependencies.forEach { dependency ->
      dependency.path("vulns").forEach { vulnerability ->
        val severity = when {
          vulnerability.hasNonNull("severity") -> SecuritySeverity.fromText(vulnerability.path("severity").asText())
          vulnerability.path("severity").hasNonNull("name") -> SecuritySeverity.fromText(vulnerability.path("severity").path("name").asText())
          else -> SecuritySeverity.UNKNOWN
        }
        incrementSeverity(counts, severity)
      }
    }
    return counts.filterValues { it > 0 }
  }

  private fun parseCargoAudit(root: JsonNode): Map<SecuritySeverity, Int> {
    val counts = mutableSeverityCounts()
    root.path("vulnerabilities").path("list").forEach { vulnerability ->
      val advisory = vulnerability.path("advisory")
      val severity = SecuritySeverity.fromText(advisory.path("cvss").path("severity").asText(null))
        ?: SecuritySeverity.fromText(advisory.path("severity").asText(null))
        ?: SecuritySeverity.fromText(vulnerability.path("severity").asText(null))
        ?: SecuritySeverity.UNKNOWN
      incrementSeverity(counts, severity)
    }
    return counts.filterValues { it > 0 }
  }

  private fun parseCodeql(root: JsonNode): Map<SecuritySeverity, Int> {
    val counts = mutableSeverityCounts()
    root.path("runs").forEach { run ->
      run.path("results").forEach { result ->
        val securitySeverity = result.path("properties").path("security-severity").asText(null)
        val severity = when {
          !securitySeverity.isNullOrBlank() -> sarifSeverity(securitySeverity)
          else -> when (result.path("level").asText(null)) {
            "error" -> SecuritySeverity.HIGH
            "warning" -> SecuritySeverity.MEDIUM
            "note" -> SecuritySeverity.LOW
            else -> SecuritySeverity.UNKNOWN
          }
        }
        incrementSeverity(counts, severity)
      }
    }
    return counts.filterValues { it > 0 }
  }

  private fun sarifSeverity(value: String): SecuritySeverity {
    val numeric = value.toDoubleOrNull() ?: return SecuritySeverity.UNKNOWN
    return when {
      numeric >= 9.0 -> SecuritySeverity.CRITICAL
      numeric >= 7.0 -> SecuritySeverity.HIGH
      numeric >= 4.0 -> SecuritySeverity.MEDIUM
      else -> SecuritySeverity.LOW
    }
  }

  private fun mutableSeverityCounts(): MutableMap<SecuritySeverity, Int> =
    mutableMapOf(
      SecuritySeverity.CRITICAL to 0,
      SecuritySeverity.HIGH to 0,
      SecuritySeverity.MEDIUM to 0,
      SecuritySeverity.LOW to 0,
      SecuritySeverity.UNKNOWN to 0,
    )

  private fun aggregateSeverityCounts(summaries: List<ToolSummary>): Map<SecuritySeverity, Int> {
    val aggregate = mutableSeverityCounts()
    summaries.forEach { summary ->
      summary.severityCounts.forEach { (severity, count) ->
        incrementSeverity(aggregate, severity, count)
      }
    }
    return aggregate.filterValues { it > 0 }
  }

  private fun countAtOrAbove(
    counts: Map<SecuritySeverity, Int>,
    threshold: SecuritySeverity,
  ): Int = counts.entries.sumOf { (severity, count) -> if (severity.breaches(threshold)) count else 0 }

  private fun severityCountData(counts: Map<SecuritySeverity, Int>): Map<String, Int> =
    counts.entries.associate { (severity, count) -> severity.name.lowercase() to count }

  private fun incrementSeverity(
    counts: MutableMap<SecuritySeverity, Int>,
    severity: SecuritySeverity?,
    amount: Int = 1,
  ) {
    val resolved = severity ?: SecuritySeverity.UNKNOWN
    counts[resolved] = counts.getOrDefault(resolved, 0) + amount
  }

  private fun validateTools(
    toolIds: List<String>,
    supportedTools: Set<SecurityTool>,
    description: String,
  ): List<SecurityTool> {
    if (toolIds.isEmpty()) {
      return emptyList()
    }
    return toolIds.distinct().map { toolId ->
      val tool = SecurityTool.fromId(toolId)
        ?: throw IllegalArgumentException(
          "Unsupported $description entry '$toolId'. Supported values: ${supportedTools.joinToString(", ") { it.id }}.",
        )
      require(tool in supportedTools) {
        "Unsupported $description entry '$toolId'. Supported values: ${supportedTools.joinToString(", ") { it.id }}."
      }
      tool
    }
  }

  private fun detectAuditTools(workingDir: Path): List<SecurityTool> {
    val tools = mutableListOf<SecurityTool>()
    if (Files.exists(workingDir.resolve("package.json"))) tools += SecurityTool.NPM_AUDIT
    if (
      Files.exists(workingDir.resolve("requirements.txt")) ||
      Files.exists(workingDir.resolve("pyproject.toml")) ||
      Files.exists(workingDir.resolve("Pipfile"))
    ) {
      tools += SecurityTool.PIP_AUDIT
    }
    if (Files.exists(workingDir.resolve("Cargo.toml"))) tools += SecurityTool.CARGO_AUDIT
    return tools
  }

  private fun resolveRelativePath(projectContext: ProjectContext, candidate: String, description: String): Path =
    projectContext.resolvePath(Path.of(context.workingDirectory).resolve(candidate).normalize().toString(), description)

  private fun appendArgs(args: List<String>): String =
    if (args.isEmpty()) "" else " ${ShellArgumentSanitizer.escapeShellArgs(args)}"

  private fun severityLevelsAtOrAbove(threshold: SecuritySeverity): String {
    val ordered = listOf(SecuritySeverity.LOW, SecuritySeverity.MEDIUM, SecuritySeverity.HIGH, SecuritySeverity.CRITICAL)
    return ordered.filter { it.breaches(threshold) }.joinToString(",") { it.name }
  }

  private fun commandFailureMessage(result: CommandResult): String =
    result.stderr.takeIf { it.isNotBlank() }
      ?: result.stdout.takeIf { it.isNotBlank() }
      ?: "exit code ${result.exitCode}"

  enum class Operation(val taskId: String, val description: String) {
    SCAN("security-scan", "Run security scanners such as Trivy, Snyk, or CodeQL"),
    AUDIT("security-audit", "Audit project dependencies with npm audit, pip-audit, or cargo audit"),
    SBOM("security-sbom", "Generate a CycloneDX or SPDX SBOM for the project"),
  }

  private data class ExecutionPlan(
    val executions: List<ToolExecution> = emptyList(),
    val threshold: SecuritySeverity? = null,
    val skipReason: String? = null,
    val skippedTools: List<String> = emptyList(),
  )

  private data class ToolExecution(
    val tool: SecurityTool,
    val command: String,
    val artifactPath: Path? = null,
  )

  private data class ToolSummary(
    val tool: SecurityTool,
    val command: String,
    val exitCode: Int? = null,
    val severityCounts: Map<SecuritySeverity, Int> = emptyMap(),
    val issuesAtOrAboveThreshold: Int = 0,
    val error: String? = null,
  ) {
    fun toData(): Map<String, Any> = buildMap {
      put("tool", tool.id)
      put("command", command)
      exitCode?.let { put("exitCode", it) }
      if (severityCounts.isNotEmpty()) {
        put("severityCounts", severityCounts.entries.associate { (severity, count) -> severity.name.lowercase() to count })
        put("issueCount", severityCounts.values.sum())
        put("issuesAtOrAboveThreshold", issuesAtOrAboveThreshold)
      }
      error?.let { put("error", it) }
    }
  }

  private enum class ToolKind {
    SCAN,
    AUDIT,
    SBOM,
  }

  private enum class SecurityTool(val id: String, val kind: ToolKind) {
    TRIVY("trivy", ToolKind.SCAN),
    SNYK("snyk", ToolKind.SCAN),
    CODEQL("codeql", ToolKind.SCAN),
    NPM_AUDIT("npm-audit", ToolKind.AUDIT),
    PIP_AUDIT("pip-audit", ToolKind.AUDIT),
    CARGO_AUDIT("cargo-audit", ToolKind.AUDIT),
    ;

    companion object {
      fun fromId(id: String): SecurityTool? =
        when (id.trim().lowercase()) {
          "trivy" -> TRIVY
          "snyk" -> SNYK
          "codeql" -> CODEQL
          "npm-audit", "npm_audit", "npm" -> NPM_AUDIT
          "pip-audit", "pip_audit", "pip" -> PIP_AUDIT
          "cargo-audit", "cargo_audit", "cargo" -> CARGO_AUDIT
          else -> null
        }
    }
  }

  companion object {
    private val mapper = ObjectMapper()
    private val AUDIT_TOOLS = setOf(SecurityTool.NPM_AUDIT, SecurityTool.PIP_AUDIT, SecurityTool.CARGO_AUDIT)
    private val SCAN_AND_AUDIT_TOOLS = setOf(
      SecurityTool.TRIVY,
      SecurityTool.SNYK,
      SecurityTool.CODEQL,
      SecurityTool.NPM_AUDIT,
      SecurityTool.PIP_AUDIT,
      SecurityTool.CARGO_AUDIT,
    )
  }
}
