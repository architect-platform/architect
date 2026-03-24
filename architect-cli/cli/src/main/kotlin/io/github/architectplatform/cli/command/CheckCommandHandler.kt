package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import kotlin.system.exitProcess

/**
 * Handles the `architect check` command: runs task precondition checks without executing.
 */
class CheckCommandHandler(
  private val embeddedTaskExecutor: EmbeddedTaskExecutor,
  private val extractProjectName: (String) -> String,
) {

  var json: Boolean = false

  fun handle(args: List<String>) {
    val taskFilter = args.getOrNull(1)
    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    val context = io.github.architectplatform.core.execution.EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val allTasks = context.getAllTasks(projectName)
    val checker = io.github.architectplatform.core.tasks.application.TaskConditionChecker()

    val tasksToCheck = if (taskFilter != null) {
      allTasks.filter { it.id == taskFilter }.also {
        if (it.isEmpty()) {
          println("No task found with id '$taskFilter'")
          exitProcess(1)
        }
      }
    } else {
      allTasks
    }

    val results = checker.checkAll(tasksToCheck)

    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results))
      return
    }

    val ok = results.filter { it.satisfied }
    val blocked = results.filter { !it.satisfied }

    println()
    println("━".repeat(80))
    println("🔍 Task Precondition Check — $projectName")
    println("━".repeat(80))
    println()

    if (blocked.isNotEmpty()) {
      println("  ❌ BLOCKED (${blocked.size})")
      println()
      blocked.forEach { result ->
        println("  ╔══ ${result.taskId}")
        result.issues.forEach { issue ->
          println("  ║  ⚠  ${issue.message}")
          println("  ║     → ${issue.hint}")
        }
        println("  ╚══")
        println()
      }
    }

    if (ok.isNotEmpty()) {
      println("  ✅ READY (${ok.size})")
      ok.chunked(4).forEach { chunk ->
        println("    " + chunk.joinToString("  ") { it.taskId })
      }
      println()
    }

    println("  ${ok.size} ready, ${blocked.size} blocked")
    println()

    if (blocked.isNotEmpty()) exitProcess(1)
  }
}
