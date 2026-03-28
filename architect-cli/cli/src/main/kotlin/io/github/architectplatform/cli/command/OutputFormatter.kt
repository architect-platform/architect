package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.MonorepoHealthDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskStatsDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.cli.graph.ProjectGraphDotRenderer
import io.github.architectplatform.cli.graph.ProjectGraphHtmlRenderer
import io.github.architectplatform.cli.graph.TaskGraphDotRenderer
import io.github.architectplatform.cli.graph.TaskGraphHtmlRenderer
import io.github.architectplatform.cli.graph.TaskPlanTreeRenderer
import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import java.awt.Desktop
import kotlin.system.exitProcess

/**
 * Handles output formatting, graph rendering, and display of CLI results.
 */
class OutputFormatter(
  private val projectGraphDotRenderer: ProjectGraphDotRenderer = ProjectGraphDotRenderer(),
  private val projectGraphHtmlRenderer: ProjectGraphHtmlRenderer = ProjectGraphHtmlRenderer(),
  private val taskGraphDotRenderer: TaskGraphDotRenderer = TaskGraphDotRenderer(),
  private val taskGraphHtmlRenderer: TaskGraphHtmlRenderer = TaskGraphHtmlRenderer(),
  private val taskPlanTreeRenderer: TaskPlanTreeRenderer = TaskPlanTreeRenderer(),
) {

  var json: Boolean = false
  var filter: String? = null
  var verbosity: Int = 0

  fun printPlan(plan: TaskPlanDTO) {
    println()
    println("━".repeat(80))
    println("📋 Execution Plan: ${plan.task}")
    println("📦 Project: ${plan.project}")
    println("━".repeat(80))
    println()
    println("  ${plan.totalSteps} task(s) across ${plan.parallelBatches} parallel batch(es)")
    println()

    val byBatch = plan.steps.groupBy { it.batch }.toSortedMap()
    byBatch.forEach { (batchIdx, tasks) ->
      val batchLabel = if (tasks.size > 1)
        "Batch $batchIdx — running ${tasks.size} tasks in parallel"
      else
        "Batch $batchIdx"
      println("  ┌─ $batchLabel")
      tasks.forEachIndexed { i, step ->
        val connector = if (i == tasks.size - 1) "└──" else "├──"
        val phase = if (step.phase != null) " [${step.phase}]" else ""
        val deps = if (step.depends.isNotEmpty()) " ← ${step.depends.joinToString(", ")}" else ""
        println("  │  $connector ${step.id}$phase  ${step.description}$deps")
      }
      println("  │")
    }
    println()
  }

  fun printPlanTree(plan: TaskPlanDTO) {
    print(taskPlanTreeRenderer.render(plan))
  }

  fun printGraph(projectName: String, plans: List<TaskPlanDTO>) {
    println(taskGraphDotRenderer.render(projectName, plans))
  }

  fun printProjectGraph(projectName: String, graph: ProjectDependencyGraph) {
    println(projectGraphDotRenderer.render("$projectName-projects", graph))
  }

  fun openGraph(projectName: String, plans: List<TaskPlanDTO>) {
    val outputPath = taskGraphHtmlRenderer.writeTempFile(projectName, plans)
    println("📈 Graph page: $outputPath")
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop().browse(outputPath.toUri())
    } else {
      println("Browser opening is not supported in this environment. Open the HTML file manually.")
    }
  }

  fun openProjectGraph(
    projectName: String,
    graph: ProjectDependencyGraph,
    affectedProjects: Set<String> = emptySet(),
  ) {
    val outputPath = projectGraphHtmlRenderer.writeTempFile("$projectName-projects", graph, affectedProjects)
    println("📈 Project graph page: $outputPath")
    if (affectedProjects.isNotEmpty()) {
      println("  🔥 Highlighting ${affectedProjects.size} affected project(s): ${affectedProjects.sorted().joinToString(", ")}")
    }
    val cycles = graph.detectCycles()
    if (cycles.isNotEmpty()) {
      println("  ⚠  ${cycles.size} circular dependency cycle(s) detected (shown in red)")
    }
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop().browse(outputPath.toUri())
    } else {
      println("Browser opening is not supported in this environment. Open the HTML file manually.")
    }
  }

  fun printValidation(projectName: String, result: ValidationResultDTO) {
    println()
    println("━".repeat(80))
    val status = if (result.valid) "✅ VALID" else "❌ INVALID"
    println("$status — Project: $projectName")
    println("━".repeat(80))
    if (result.errors.isEmpty() && result.warnings.isEmpty()) {
      println("  No issues found.")
    }
    result.errors.forEach { println("  ❌ ERROR:   $it") }
    result.warnings.forEach { println("  ⚠️  WARNING: $it") }
    println()
  }

  fun printStats(statsList: List<TaskStatsDTO>) {
    if (statsList.isEmpty()) {
      println("No performance data found.")
      return
    }
    println()
    println("━".repeat(90))
    println("📊 Task Performance Statistics")
    println("━".repeat(90))
    val fmt = "%-30s  %6s  %8s  %8s  %8s  %8s  %7s  %s"
    println(fmt.format("TASK", "RUNS", "AVG", "P50", "P95", "P99", "SUCCESS", "TREND"))
    println("─".repeat(90))
    statsList.forEach { s ->
      val trend = when (s.trend) {
        "IMPROVING" -> "▼ faster"
        "DEGRADING" -> "▲ slower"
        else -> "→ stable"
      }
      println(
        fmt.format(
          s.taskId.take(30),
          s.sampleCount,
          fmtMs(s.avgDurationMs),
          fmtMs(s.p50DurationMs),
          fmtMs(s.p95DurationMs),
          fmtMs(s.p99DurationMs),
          "${(s.successRate * 100).toInt()}%",
          trend,
        )
      )
    }
    println()
  }

  fun printTaskStats(stats: TaskStatsDTO) {
    val trend = when (stats.trend) {
      "IMPROVING" -> "▼ getting faster"
      "DEGRADING" -> "▲ getting slower"
      else -> "→ stable"
    }
    println()
    println("━".repeat(60))
    println("📊 Performance Profile: ${stats.taskId}")
    println("━".repeat(60))
    println("  Project      : ${stats.project}")
    println("  Samples      : ${stats.sampleCount}")
    println("  Success rate : ${(stats.successRate * 100).toInt()}%")
    println()
    println("  Latency")
    println("  ─────────────────────────")
    println("  Min   : ${fmtMs(stats.minDurationMs)}")
    println("  Avg   : ${fmtMs(stats.avgDurationMs)}")
    println("  p50   : ${fmtMs(stats.p50DurationMs)}")
    println("  p95   : ${fmtMs(stats.p95DurationMs)}")
    println("  p99   : ${fmtMs(stats.p99DurationMs)}")
    println("  Max   : ${fmtMs(stats.maxDurationMs)}")
    println()
    println("  Trend : $trend")
    if (stats.sampleCount >= 5) {
      println("  Recent avg (last 10) : ${fmtMs(stats.recentAvgMs)}")
      println("  Historical avg       : ${fmtMs(stats.historicalAvgMs)}")
    }
    println()
  }

  private fun fmtMs(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    ms < 60_000 -> "${"%.1f".format(ms / 1000.0)}s"
    else -> "${"%.1f".format(ms / 60_000.0)}m"
  }

  fun printHistory(records: List<HistoryRecordDTO>) {
    if (records.isEmpty()) {
      println("No execution history found.")
      return
    }
    println()
    println("━".repeat(80))
    println("📜 Execution History")
    println("━".repeat(80))
    val fmt = "%-8s  %-18s  %-14s  %-7s  %s"
    println(fmt.format("STATUS", "WHEN", "PROJECT", "DURATION", "TASK"))
    println("─".repeat(80))
    records.forEach { r ->
      val status = if (r.success) "✅" else "❌"
      val when_ = java.time.Instant.ofEpochMilli(r.timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
      val duration = "${r.durationMs / 1000}.${(r.durationMs % 1000) / 100}s"
      println(fmt.format(status, when_, r.project, duration, r.task))
    }
    println()
  }

  fun printTasks(tasks: List<io.github.architectplatform.cli.dto.TaskDTO>) {
    val taskEntries = tasks.filter { it.groupMembers == null }
    var filteredTasks = taskEntries
    if (filter != null) {
      val f = filter!!.uppercase()
      filteredTasks = taskEntries.filter { it.phase?.uppercase() == f }
    }

    val tasksById = filteredTasks.associateBy { it.id }
    val groupHeaders =
      tasks.filter { it.groupMembers != null }
        .mapNotNull { header ->
          val members = header.groupMembers.orEmpty().filter { tasksById.containsKey(it) }
          if (members.isEmpty()) null else header.copy(groupMembers = members)
        }
    val rendered = if (groupHeaders.isEmpty()) filteredTasks else groupHeaders + filteredTasks

    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rendered))
      return
    }

    println()
    println("━".repeat(80))
    println("🧭 Available Tasks" + if (filter != null) " (phase: ${filter!!.uppercase()})" else "")
    println("━".repeat(80))
    val fmt = "  %-30s  %-12s  %s"
    println(fmt.format("TASK", "PHASE", "DESCRIPTION"))
    println("  ${"─".repeat(76)}")
    if (groupHeaders.isEmpty()) {
      filteredTasks.forEach { t ->
        println(fmt.format(t.id.take(30), (t.phase ?: "—").take(12), t.description.take(34)))
      }
    } else {
      val groupedMembers = linkedSetOf<String>()
      groupHeaders.forEach { group ->
        println(fmt.format(group.id.take(30), "GROUP", group.description.take(34)))
        group.groupMembers.orEmpty().forEach { memberId ->
          val member = tasksById[memberId]
          val label = "↳ $memberId"
          if (member != null) {
            println(
              fmt.format(label.take(30), (member.phase ?: "—").take(12), member.description.take(34)),
            )
          } else {
            println(fmt.format(label.take(30), "—", ""))
          }
          groupedMembers += memberId
        }
      }
      filteredTasks
        .filterNot { it.id in groupedMembers }
        .forEach { t ->
          println(fmt.format(t.id.take(30), (t.phase ?: "—").take(12), t.description.take(34)))
        }
    }
    println()
    println("  ${filteredTasks.size} task(s) available")
    println()
  }

  fun printInfo(projectName: String, projectPath: String, tasks: List<io.github.architectplatform.cli.dto.TaskDTO>) {
    val taskEntries = tasks.filter { it.groupMembers == null }
    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      val info = mapOf(
        "project" to projectName,
        "path" to projectPath,
        "taskCount" to taskEntries.size,
        "tasks" to tasks,
        "phases" to taskEntries.mapNotNull { it.phase }.distinct().sorted(),
      )
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info))
      return
    }

    println()
    println("━".repeat(80))
    println("ℹ️  Project Info")
    println("━".repeat(80))
    println("  Name:   $projectName")
    println("  Path:   $projectPath")
    println("  Tasks:  ${taskEntries.size}")
    val phases = taskEntries.mapNotNull { it.phase }.distinct().sorted()
    if (phases.isNotEmpty()) {
      println("  Phases: ${phases.joinToString(", ")}")
    }
    println()
  }

  fun printVersion() {
    val cliVersion = io.github.architectplatform.cli.CliVersion.current()
    if (json) {
      println("""{"cli":"$cliVersion"}""")
      return
    }
    println("architect $cliVersion")
  }

  fun printAffected(affectedProjects: Set<String>, baseRef: String?) {
    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(affectedProjects))
      return
    }
    if (affectedProjects.isEmpty()) {
      println("✅ No projects affected.")
      return
    }
    println()
    println("━".repeat(80))
    println("🎯 Affected Projects (base: ${baseRef ?: "HEAD~1"})")
    println("━".repeat(80))
    affectedProjects.sorted().forEach { println("  • $it") }
    println()
    println("  ${affectedProjects.size} project(s) affected")
    println()
  }

  fun printHealthDashboard(health: MonorepoHealthDTO, jsonOutput: Boolean = false) {
    if (jsonOutput || json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(health))
      return
    }

    val overallIcon = if (health.healthy) "✅" else "❌"
    println()
    println("━".repeat(80))
    println("$overallIcon  Monorepo Health — ${health.rootProject}")
    println("━".repeat(80))
    println("  Projects: ${health.totalProjects}  |  Healthy: ${health.healthyCount}  |  Unhealthy: ${health.unhealthyCount}")
    println()

    health.projects.forEach { proj ->
      val statusIcon = if (proj.valid) "✅" else "❌"
      val buildBadge = when (proj.lastBuildSuccess) {
        true -> " 🏗 built"
        false -> " 🔴 build failed"
        null -> ""
      }
      val ageBadge = proj.lastBuildAgeSeconds?.let { age ->
        when {
          age < 60 -> " (${age}s ago)"
          age < 3600 -> " (${age / 60}m ago)"
          age < 86400 -> " (${age / 3600}h ago)"
          else -> " (${age / 86400}d ago)"
        }
      } ?: ""
      println("  $statusIcon ${proj.name}  — ${proj.taskCount} tasks$buildBadge$ageBadge")
      proj.errors.forEach { println("     ❌ $it") }
      proj.warnings.forEach { println("     ⚠️  $it") }
    }

    println()
    println("  Timestamp: ${health.timestamp}")
    println()
  }

  fun parseGraphOptions(arguments: List<String>): GraphOptions {
    var open = false
    var showProjects = false
    var showAffected = false
    val positional = mutableListOf<String>()
    arguments.drop(1).forEach { argument ->
      when (argument) {
        "--open" -> open = true
        "--projects" -> showProjects = true
        "--affected" -> showAffected = true
        else -> if (argument.startsWith("--")) {
          println("Usage: architect graph [task] [--projects] [--affected] [--open]")
          exitProcess(1)
        } else {
          positional += argument
        }
      }
    }
    if (positional.size > 1 || (showProjects && positional.isNotEmpty())) {
      println("Usage: architect graph [task] [--projects] [--affected] [--open]")
      exitProcess(1)
    }
    return GraphOptions(open = open, taskName = positional.singleOrNull(), showProjects = showProjects, showAffected = showAffected)
  }

  fun parsePlanOptions(arguments: List<String>): PlanOptions {
    var tree = false
    val positional = mutableListOf<String>()
    arguments.drop(1).forEach { argument ->
      when (argument) {
        "--tree" -> tree = true
        else -> if (argument.startsWith("--")) {
          println("Usage: architect plan <task> [--tree]")
          exitProcess(1)
        } else {
          positional += argument
        }
      }
    }
    return PlanOptions(taskName = positional.singleOrNull(), tree = tree)
  }

  data class GraphOptions(
    val open: Boolean,
    val taskName: String?,
    val showProjects: Boolean,
    val showAffected: Boolean = false,
  )

  data class PlanOptions(
    val taskName: String?,
    val tree: Boolean,
  )
}
