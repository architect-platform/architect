package io.github.architectplatform.plugins.testing

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
import java.util.Locale
import kotlin.io.path.extension
import kotlin.io.path.name

class TestingTask(
  private val suite: Suite,
  private val phase: Phase,
  private val context: TestingContext,
) : Task {
  override val id: String = suite.taskId

  override fun phase(): Phase = phase

  override fun description(): String = suite.description

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.skipped("Testing tasks disabled")
    }

    val workingDir = try {
      projectContext.resolvePath(context.workingDirectory, "Testing working directory")
    } catch (e: IllegalArgumentException) {
      return TaskResult.failure(
        "Testing task $id failed with invalid working directory: ${e.message}",
      )
    }

    val framework = try {
      detectFramework(workingDir, context)
    } catch (e: IllegalArgumentException) {
      return TaskResult.failure("Testing task $id failed: ${e.message}")
    } ?: return TaskResult.skipped("Unable to auto-detect a supported test framework in $workingDir")

    val plan = buildExecutionPlan(framework, workingDir, args)
    plan.skipReason?.let { return TaskResult.skipped(it) }

    val command = requireNotNull(plan.command)
    val executor = environment.service(CommandExecutor::class.java)
    if (suite != Suite.COVERAGE) {
      return try {
        executor.execute(command, workingDir.toString())
        TaskResult.success(
          "Testing task $id completed using ${framework.id}",
          data = baseData(framework, command, workingDir),
        )
      } catch (e: Exception) {
        TaskResult.failure(
          "Testing task $id failed for ${framework.id}: ${e.message ?: "Unknown error"}",
          data = baseData(framework, command, workingDir),
        )
      }
    }

    val commandResult = executor.executeWithResult(command, workingDir.toString())
    val coverageLookup = collectCoverageSummary(projectContext, workingDir, context.coverage.reportPaths, commandResult)
    coverageLookup.error?.let {
      return TaskResult.failure(
        "Testing task $id failed: $it",
        data = baseData(framework, command, workingDir),
      )
    }

    val summary = coverageLookup.summary
      ?: return TaskResult.failure(
        "Testing task $id failed: no coverage summary was produced",
        data = baseData(framework, command, workingDir),
      )

    val coverageData = baseData(framework, command, workingDir) + mapOf(
      "coveragePercent" to summary.percent,
      "threshold" to context.coverage.threshold,
      "coverageReports" to summary.reportPaths,
      "coverageSource" to summary.source,
      "reportCount" to summary.reportPaths.size,
    )

    if (!commandResult.success) {
      return TaskResult.failure(
        "Testing task $id failed for ${framework.id}: ${commandFailureMessage(commandResult)}",
        data = coverageData,
      )
    }

    if (summary.percent < context.coverage.threshold.toDouble()) {
      return TaskResult.failure(
        "Coverage ${formatPercent(summary.percent)} is below threshold ${context.coverage.threshold}%",
        data = coverageData,
      )
    }

    return TaskResult.success(
      "Coverage ${formatPercent(summary.percent)} meets threshold ${context.coverage.threshold}%",
      data = coverageData,
    )
  }

  private fun baseData(framework: DetectedFramework, command: String, workingDir: Path): Map<String, Any> = mapOf(
    "framework" to framework.id,
    "suite" to suite.taskId,
    "command" to command,
    "workingDirectory" to workingDir.toString(),
    "parallel" to context.parallel,
    "retryFlaky" to context.retryFlaky,
  )

  private fun buildExecutionPlan(
    framework: DetectedFramework,
    workingDir: Path,
    args: List<String>,
  ): ExecutionPlan {
    if (suite == Suite.COVERAGE && !context.coverage.enabled) {
      return ExecutionPlan(skipReason = "Coverage execution disabled")
    }

    val override = suite.overrideCommand(context)
    if (!override.isNullOrBlank()) {
      return ExecutionPlan(command = appendArgs(override, args))
    }

    return when (framework) {
      DetectedFramework.JUNIT -> gradlePlan(workingDir, args)
      DetectedFramework.PYTEST -> pythonPlan(workingDir, args)
      DetectedFramework.JEST -> jestPlan(workingDir, args)
      DetectedFramework.VITEST -> vitestPlan(workingDir, args)
      DetectedFramework.GO -> goPlan(workingDir, args)
      DetectedFramework.CARGO -> cargoPlan(workingDir, args)
    }
  }

  private fun gradlePlan(workingDir: Path, args: List<String>): ExecutionPlan {
    val gradleExecutable = if (Files.exists(workingDir.resolve("gradlew"))) "./gradlew" else "gradle"
    val parallelFlag = if (context.parallel) " --parallel" else ""
    val argsString = appendArgs("", args)
    return when (suite) {
      Suite.UNIT -> ExecutionPlan(command = "$gradleExecutable$parallelFlag test$argsString")
      Suite.INTEGRATION -> when {
        gradleTaskExists(workingDir, "integrationTest") -> ExecutionPlan(command = "$gradleExecutable$parallelFlag integrationTest$argsString")
        else -> ExecutionPlan(skipReason = "No Gradle integration test task detected")
      }
      Suite.E2E -> when {
        gradleTaskExists(workingDir, "e2eTest") -> ExecutionPlan(command = "$gradleExecutable$parallelFlag e2eTest$argsString")
        gradleTaskExists(workingDir, "functionalTest") -> ExecutionPlan(command = "$gradleExecutable$parallelFlag functionalTest$argsString")
        gradleTaskExists(workingDir, "endToEndTest") -> ExecutionPlan(command = "$gradleExecutable$parallelFlag endToEndTest$argsString")
        else -> ExecutionPlan(skipReason = "No Gradle e2e/functional test task detected")
      }
      Suite.COVERAGE -> {
        val coverageCommand =
          if (gradleTaskExists(workingDir, "jacocoTestReport")) "$gradleExecutable$parallelFlag test jacocoTestReport"
          else "$gradleExecutable$parallelFlag test"
        ExecutionPlan(command = "$coverageCommand$argsString")
      }
    }
  }

  private fun pythonPlan(workingDir: Path, args: List<String>): ExecutionPlan =
    when (suite) {
      Suite.UNIT -> ExecutionPlan(command = "pytest${appendArgs("", args)}")
      Suite.INTEGRATION -> conventionalPathPlan(workingDir, "pytest", args, listOf("tests/integration", "test/integration", "integration"))
      Suite.E2E -> conventionalPathPlan(workingDir, "pytest", args, listOf("tests/e2e", "test/e2e", "e2e", "tests/end_to_end", "tests/end-to-end"))
      Suite.COVERAGE -> {
        val reportFlags = pytestCoverageFlags(context.coverage.normalizedReporters())
        ExecutionPlan(command = "pytest --cov$reportFlags${appendArgs("", args)}")
      }
    }

  private fun jestPlan(workingDir: Path, args: List<String>): ExecutionPlan {
    val serialFlag = if (context.parallel) "" else " --runInBand"
    val coverageReporterFlags = jestCoverageFlags(context.coverage.normalizedReporters())
    return when (suite) {
      Suite.UNIT -> ExecutionPlan(command = "npx jest$serialFlag${appendArgs("", args)}")
      Suite.INTEGRATION -> conventionalPathPlan(workingDir, "npx jest$serialFlag", args, listOf("tests/integration", "test/integration", "integration"))
      Suite.E2E -> conventionalPathPlan(workingDir, "npx jest$serialFlag", args, listOf("tests/e2e", "test/e2e", "e2e", "tests/end_to_end", "tests/end-to-end"))
      Suite.COVERAGE -> ExecutionPlan(command = "npx jest$serialFlag --coverage$coverageReporterFlags${appendArgs("", args)}")
    }
  }

  private fun vitestPlan(workingDir: Path, args: List<String>): ExecutionPlan {
    val serialFlag = if (context.parallel) "" else " --maxWorkers=1"
    val retryFlag = if (context.retryFlaky > 0) " --retry=${context.retryFlaky}" else ""
    val coverageReporterFlags = vitestCoverageFlags(context.coverage.normalizedReporters())
    return when (suite) {
      Suite.UNIT -> ExecutionPlan(command = "npx vitest run$serialFlag$retryFlag${appendArgs("", args)}")
      Suite.INTEGRATION -> conventionalPathPlan(workingDir, "npx vitest run$serialFlag$retryFlag", args, listOf("tests/integration", "test/integration", "integration"))
      Suite.E2E -> conventionalPathPlan(workingDir, "npx vitest run$serialFlag$retryFlag", args, listOf("tests/e2e", "test/e2e", "e2e", "tests/end_to_end", "tests/end-to-end"))
      Suite.COVERAGE -> ExecutionPlan(command = "npx vitest run$serialFlag$retryFlag --coverage$coverageReporterFlags${appendArgs("", args)}")
    }
  }

  private fun goPlan(workingDir: Path, args: List<String>): ExecutionPlan {
    val parallelFlag = if (context.parallel) "" else " -p 1"
    return when (suite) {
      Suite.UNIT -> ExecutionPlan(command = "go test$parallelFlag${appendArgs("", args)} ./...")
      Suite.INTEGRATION -> goDirectoryPlan(workingDir, args, listOf("tests/integration", "test/integration", "integration"), parallelFlag)
      Suite.E2E -> goDirectoryPlan(workingDir, args, listOf("tests/e2e", "test/e2e", "e2e", "tests/end_to_end", "tests/end-to-end"), parallelFlag)
      Suite.COVERAGE -> ExecutionPlan(command = "go test$parallelFlag${appendArgs("", args)} ./... -coverprofile=cover.out && go tool cover -func=cover.out")
    }
  }

  private fun cargoPlan(workingDir: Path, args: List<String>): ExecutionPlan =
    when (suite) {
      Suite.UNIT -> ExecutionPlan(command = "cargo test${appendArgs("", args)}")
      Suite.INTEGRATION -> if (Files.isDirectory(workingDir.resolve("tests"))) {
        ExecutionPlan(command = "cargo test --tests${appendArgs("", args)}")
      } else {
        ExecutionPlan(skipReason = "No Cargo integration test targets detected")
      }
      Suite.E2E -> {
        val e2eTarget = listOf("tests/e2e.rs", "tests/end_to_end.rs", "tests/end-to-end.rs")
          .map(workingDir::resolve)
          .firstOrNull(Files::exists)
        if (e2eTarget != null) {
          val safeTarget = ShellArgumentSanitizer.requireSafeIdentifier(e2eTarget.name.removeSuffix(".rs"), "Cargo e2e test target")
          ExecutionPlan(command = "cargo test --test $safeTarget${appendArgs("", args)}")
        } else {
          ExecutionPlan(skipReason = "No Cargo e2e test target detected")
        }
      }
      Suite.COVERAGE -> {
        val reporters = context.coverage.normalizedReporters()
        val command =
          if ("cobertura" in reporters && "lcov" !in reporters) {
            "cargo llvm-cov --cobertura --output-path coverage.xml"
          } else {
            "cargo llvm-cov --lcov --output-path lcov.info"
          }
        ExecutionPlan(command = "$command${appendArgs("", args)}")
      }
    }

  private fun pytestCoverageFlags(reporters: List<String>): String {
    val flags = mutableListOf("--cov-report=term-missing")
    if ("html" in reporters) flags += "--cov-report=html:htmlcov"
    if ("cobertura" in reporters || "xml" in reporters) flags += "--cov-report=xml:coverage.xml"
    if ("lcov" in reporters) flags += "--cov-report=lcov:lcov.info"
    return flags.joinToString(prefix = " ", separator = " ")
  }

  private fun jestCoverageFlags(reporters: List<String>): String =
    reporters
      .distinct()
      .joinToString(separator = "") { reporter ->
        val normalized = if (reporter == "text-summary") "text-summary" else reporter
        " --coverageReporters=${ShellArgumentSanitizer.requireSafeIdentifier(normalized, "Jest coverage reporter")}"
      }

  private fun vitestCoverageFlags(reporters: List<String>): String =
    reporters
      .distinct()
      .joinToString(separator = "") { reporter ->
        val normalized = if (reporter == "text") "text" else reporter
        " --coverage.reporter=${ShellArgumentSanitizer.requireSafeIdentifier(normalized, "Vitest coverage reporter")}"
      }

  private fun conventionalPathPlan(
    workingDir: Path,
    baseCommand: String,
    args: List<String>,
    candidates: List<String>,
  ): ExecutionPlan {
    val selected = candidates.firstOrNull { Files.exists(workingDir.resolve(it)) }
      ?: return ExecutionPlan(skipReason = "No ${suite.taskId.removePrefix("test-")} test path detected")
    return ExecutionPlan(command = "$baseCommand ${ShellArgumentSanitizer.escapeShellArg(selected)}${appendArgs("", args)}")
  }

  private fun goDirectoryPlan(
    workingDir: Path,
    args: List<String>,
    candidates: List<String>,
    parallelFlag: String,
  ): ExecutionPlan {
    val selected = candidates.firstOrNull { Files.exists(workingDir.resolve(it)) }
      ?: return ExecutionPlan(skipReason = "No ${suite.taskId.removePrefix("test-")} Go test path detected")
    return ExecutionPlan(command = "go test$parallelFlag${appendArgs("", args)} ./$selected/...")
  }

  private fun appendArgs(prefixCommand: String, args: List<String>): String {
    val escapedArgs = if (args.isNotEmpty()) " ${ShellArgumentSanitizer.escapeShellArgs(args)}" else ""
    return prefixCommand + escapedArgs
  }

  private fun gradleTaskExists(workingDir: Path, taskName: String): Boolean {
    val buildFiles = listOf("build.gradle.kts", "build.gradle")
      .map(workingDir::resolve)
      .filter(Files::exists)

    return buildFiles.any { file ->
      val content = Files.readString(file)
      Regex("""\b${Regex.escape(taskName)}\b""").containsMatchIn(content)
    }
  }

  private fun collectCoverageSummary(
    projectContext: ProjectContext,
    workingDir: Path,
    configuredReportPaths: List<String>,
    commandResult: CommandResult,
  ): CoverageLookup {
    if (configuredReportPaths.isNotEmpty()) {
      val resolved = configuredReportPaths.map {
        projectContext.resolvePath(Path.of(context.workingDirectory).resolve(it).normalize().toString(), "Coverage report path")
      }
      val missing = resolved.filterNot(Files::exists)
      if (missing.isNotEmpty()) {
        return CoverageLookup(error = "Configured coverage report(s) not found: ${missing.joinToString(", ")}")
      }
      val summary = aggregateCoverageReports(resolved)
      return if (summary != null) CoverageLookup(summary = summary) else CoverageLookup(error = "Configured coverage report(s) were not parseable")
    }

    val discovered = discoverCoverageReports(workingDir)
    if (discovered.isNotEmpty()) {
      val summary = aggregateCoverageReports(discovered)
      if (summary != null) {
        return CoverageLookup(summary = summary)
      }
    }

    val outputPercent = parseCoveragePercentFromOutput("${commandResult.stdout}\n${commandResult.stderr}")
    return if (outputPercent != null) {
      CoverageLookup(
        summary = CoverageSummary(
          percent = outputPercent,
          coveredLines = null,
          totalLines = null,
          reportPaths = emptyList(),
          source = "command-output",
        ),
      )
    } else {
      CoverageLookup(error = "No parseable coverage reports or percentages were found")
    }
  }

  private fun discoverCoverageReports(workingDir: Path): List<Path> =
    Files.walk(workingDir, 6).use { paths ->
      paths
        .filter(Files::isRegularFile)
        .filter {
          val name = it.name.lowercase()
          name == "coverage.xml" ||
            name == "lcov.info" ||
            name.contains("jacoco") && it.extension.lowercase() == "xml"
        }
        .toList()
    }

  private fun aggregateCoverageReports(reportPaths: List<Path>): CoverageSummary? {
    val parsed = reportPaths.mapNotNull(::parseCoverageReport)
    if (parsed.isEmpty()) return null

    val countBased = parsed.filter { it.coveredLines != null && it.totalLines != null }
    return if (countBased.size == parsed.size) {
      val covered = countBased.sumOf { it.coveredLines ?: 0L }
      val total = countBased.sumOf { it.totalLines ?: 0L }
      if (total == 0L) return null
      CoverageSummary(
        percent = covered.toDouble() / total.toDouble() * 100.0,
        coveredLines = covered,
        totalLines = total,
        reportPaths = reportPaths.map(Path::toString),
        source = reportType(reportPaths.first()).id,
      )
    } else {
      CoverageSummary(
        percent = parsed.map { it.percent }.average(),
        coveredLines = null,
        totalLines = null,
        reportPaths = reportPaths.map(Path::toString),
        source = reportType(reportPaths.first()).id,
      )
    }
  }

  private fun parseCoverageReport(path: Path): CoverageSummary? {
    val content = Files.readString(path)
    val jacocoCounters = Regex("""<counter[^>]+type="LINE"[^>]+missed="(\d+)"[^>]+covered="(\d+)"""")
      .findAll(content)
      .toList()
    if (jacocoCounters.isNotEmpty()) {
      val missed = jacocoCounters.sumOf { it.groupValues[1].toLong() }
      val covered = jacocoCounters.sumOf { it.groupValues[2].toLong() }
      val total = covered + missed
      if (total == 0L) return null
      return CoverageSummary(
        percent = covered.toDouble() / total.toDouble() * 100.0,
        coveredLines = covered,
        totalLines = total,
        reportPaths = listOf(path.toString()),
        source = ReportType.JACOCO.id,
      )
    }

    if (path.name.equals("coverage.xml", ignoreCase = true)) {
      val covered = Regex("""lines-covered="(\d+)"""").find(content)?.groupValues?.get(1)?.toLongOrNull()
      val valid = Regex("""lines-valid="(\d+)"""").find(content)?.groupValues?.get(1)?.toLongOrNull()
      if (covered != null && valid != null && valid > 0) {
        return CoverageSummary(
          percent = covered.toDouble() / valid.toDouble() * 100.0,
          coveredLines = covered,
          totalLines = valid,
          reportPaths = listOf(path.toString()),
          source = ReportType.COBERTURA.id,
        )
      }

      val lineRate = Regex("""line-rate="([0-9.]+)"""").find(content)?.groupValues?.get(1)?.toDoubleOrNull()
      if (lineRate != null) {
        return CoverageSummary(
          percent = lineRate * 100.0,
          coveredLines = null,
          totalLines = null,
          reportPaths = listOf(path.toString()),
          source = ReportType.COBERTURA.id,
        )
      }
    }

    if (path.name.equals("lcov.info", ignoreCase = true) || path.extension.equals("info", ignoreCase = true)) {
      var found = false
      var total = 0L
      var covered = 0L
      content.lineSequence().forEach { line ->
        when {
          line.startsWith("LF:") -> {
            total += line.removePrefix("LF:").trim().toLongOrNull() ?: 0L
            found = true
          }
          line.startsWith("LH:") -> {
            covered += line.removePrefix("LH:").trim().toLongOrNull() ?: 0L
            found = true
          }
        }
      }
      if (found && total > 0L) {
        return CoverageSummary(
          percent = covered.toDouble() / total.toDouble() * 100.0,
          coveredLines = covered,
          totalLines = total,
          reportPaths = listOf(path.toString()),
          source = ReportType.LCOV.id,
        )
      }
    }

    return null
  }

  private fun parseCoveragePercentFromOutput(output: String): Double? =
    Regex("""(\d+(?:\.\d+)?)%""")
      .findAll(output)
      .mapNotNull { it.groupValues[1].toDoubleOrNull() }
      .lastOrNull()

  private fun commandFailureMessage(commandResult: CommandResult): String =
    listOf(commandResult.stderr.trim(), commandResult.stdout.trim())
      .firstOrNull { it.isNotBlank() }
      ?: "exit code ${commandResult.exitCode}"

  private fun formatPercent(percent: Double): String =
    String.format(Locale.ROOT, "%.2f%%", percent)

  companion object {
    fun detectFramework(
      workingDir: Path,
      context: TestingContext = TestingContext(),
    ): DetectedFramework? {
      val explicit = when (context.normalizedFramework()) {
        "junit" -> DetectedFramework.JUNIT
        "junit-gradle" -> DetectedFramework.JUNIT
        "pytest" -> DetectedFramework.PYTEST
        "jest" -> DetectedFramework.JEST
        "vitest" -> DetectedFramework.VITEST
        "go" -> DetectedFramework.GO
        "go-test" -> DetectedFramework.GO
        "cargo" -> DetectedFramework.CARGO
        "cargo-test" -> DetectedFramework.CARGO
        "auto" -> null
        else -> throw IllegalArgumentException("Unsupported testing framework override: ${context.framework}")
      }
      if (explicit != null) return explicit

      if (hasVitestProject(workingDir)) return DetectedFramework.VITEST
      if (hasJestProject(workingDir)) return DetectedFramework.JEST
      if (hasGradleProject(workingDir)) return DetectedFramework.JUNIT
      if (hasPytestProject(workingDir)) return DetectedFramework.PYTEST
      if (Files.exists(workingDir.resolve("go.mod"))) return DetectedFramework.GO
      if (Files.exists(workingDir.resolve("Cargo.toml"))) return DetectedFramework.CARGO
      return null
    }

    private fun hasGradleProject(workingDir: Path): Boolean =
      Files.exists(workingDir.resolve("build.gradle.kts")) || Files.exists(workingDir.resolve("build.gradle"))

    private fun hasPytestProject(workingDir: Path): Boolean {
      val obviousFiles = listOf("pytest.ini", "tox.ini", "conftest.py")
      if (obviousFiles.any { Files.exists(workingDir.resolve(it)) }) return true

      val pyproject = workingDir.resolve("pyproject.toml")
      if (Files.exists(pyproject) && Files.readString(pyproject).contains("pytest")) return true

      val requirementFiles = listOf("requirements.txt", "requirements-dev.txt")
      return requirementFiles.any { file ->
        val path = workingDir.resolve(file)
        Files.exists(path) && Files.readString(path).contains("pytest")
      }
    }

    private fun hasJestProject(workingDir: Path): Boolean {
      val configFiles = listOf("jest.config.js", "jest.config.cjs", "jest.config.mjs", "jest.config.ts")
      if (configFiles.any { Files.exists(workingDir.resolve(it)) }) return true
      val packageJson = workingDir.resolve("package.json")
      return Files.exists(packageJson) && Files.readString(packageJson).contains("\"jest\"")
    }

    private fun hasVitestProject(workingDir: Path): Boolean {
      val configFiles = listOf("vitest.config.js", "vitest.config.ts", "vitest.config.mjs", "vitest.config.cjs")
      if (configFiles.any { Files.exists(workingDir.resolve(it)) }) return true
      val packageJson = workingDir.resolve("package.json")
      return Files.exists(packageJson) && Files.readString(packageJson).contains("\"vitest\"")
    }
  }

  enum class Suite(
    val taskId: String,
    val description: String,
  ) {
    UNIT("test-unit", "Run unit tests using the configured or auto-detected framework"),
    INTEGRATION("test-integration", "Run integration tests using the configured or auto-detected framework"),
    E2E("test-e2e", "Run end-to-end tests using the configured or auto-detected framework"),
    COVERAGE("test-coverage", "Run coverage and enforce the configured threshold"),
    ;

    fun overrideCommand(context: TestingContext): String? =
      when (this) {
        UNIT -> context.commands.unit
        INTEGRATION -> context.commands.integration
        E2E -> context.commands.e2e
        COVERAGE -> context.commands.coverage
      }
  }

  enum class DetectedFramework(val id: String) {
    JUNIT("junit-gradle"),
    PYTEST("pytest"),
    JEST("jest"),
    VITEST("vitest"),
    GO("go-test"),
    CARGO("cargo-test"),
  }

  private data class ExecutionPlan(
    val command: String? = null,
    val skipReason: String? = null,
  )

  private data class CoverageLookup(
    val summary: CoverageSummary? = null,
    val error: String? = null,
  )

  data class CoverageSummary(
    val percent: Double,
    val coveredLines: Long?,
    val totalLines: Long?,
    val reportPaths: List<String>,
    val source: String,
  )

  private enum class ReportType(val id: String) {
    JACOCO("jacoco"),
    COBERTURA("cobertura"),
    LCOV("lcov"),
  }

  private fun reportType(path: Path): ReportType {
    val normalized = path.toString().lowercase()
    return when {
      normalized.endsWith("lcov.info") || normalized.endsWith(".info") -> ReportType.LCOV
      normalized.contains("jacoco") -> ReportType.JACOCO
      else -> ReportType.COBERTURA
    }
  }
}
