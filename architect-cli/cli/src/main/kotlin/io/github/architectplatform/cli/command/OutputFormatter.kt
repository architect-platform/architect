package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
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

  fun openProjectGraph(projectName: String, graph: ProjectDependencyGraph) {
    val outputPath = projectGraphHtmlRenderer.writeTempFile("$projectName-projects", graph)
    println("📈 Project graph page: $outputPath")
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
    var filtered = tasks
    if (filter != null) {
      val f = filter!!.uppercase()
      filtered = tasks.filter { it.phase?.uppercase() == f }
    }

    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtered))
      return
    }

    println()
    println("━".repeat(80))
    println("🧭 Available Tasks" + if (filter != null) " (phase: ${filter!!.uppercase()})" else "")
    println("━".repeat(80))
    val fmt = "  %-30s  %-12s  %s"
    println(fmt.format("TASK", "PHASE", "DESCRIPTION"))
    println("  ${"─".repeat(76)}")
    filtered.forEach { t ->
      println(fmt.format(t.id.take(30), (t.phase ?: "—").take(12), t.description.take(34)))
    }
    println()
    println("  ${filtered.size} task(s) available")
    println()
  }

  fun printInfo(projectName: String, projectPath: String, tasks: List<io.github.architectplatform.cli.dto.TaskDTO>) {
    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      val info = mapOf(
        "project" to projectName,
        "path" to projectPath,
        "taskCount" to tasks.size,
        "tasks" to tasks,
        "phases" to tasks.mapNotNull { it.phase }.distinct().sorted(),
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
    println("  Tasks:  ${tasks.size}")
    val phases = tasks.mapNotNull { it.phase }.distinct().sorted()
    if (phases.isNotEmpty()) {
      println("  Phases: ${phases.joinToString(", ")}")
    }
    println()
  }

  fun printVersion() {
    val cliVersion = javaClass.`package`?.implementationVersion ?: "dev"
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

  fun parseGraphOptions(arguments: List<String>): GraphOptions {
    var open = false
    var showProjects = false
    val positional = mutableListOf<String>()
    arguments.drop(1).forEach { argument ->
      when (argument) {
        "--open" -> open = true
        "--projects" -> showProjects = true
        else -> if (argument.startsWith("--")) {
          println("Usage: architect graph [task] [--projects] [--open]")
          exitProcess(1)
        } else {
          positional += argument
        }
      }
    }
    if (positional.size > 1 || (showProjects && positional.isNotEmpty())) {
      println("Usage: architect graph [task] [--projects] [--open]")
      exitProcess(1)
    }
    return GraphOptions(open = open, taskName = positional.singleOrNull(), showProjects = showProjects)
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
  )

  data class PlanOptions(
    val taskName: String?,
    val tree: Boolean,
  )
}
