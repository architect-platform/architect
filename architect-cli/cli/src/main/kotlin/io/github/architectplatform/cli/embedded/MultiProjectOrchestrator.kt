package io.github.architectplatform.cli.embedded

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Orchestrates execution of a task across multiple projects in dependency order.
 *
 * Projects are grouped into execution tiers using [ProjectDependencyGraph.topologicalTiers].
 * Within each tier, projects run in parallel. Tiers execute sequentially.
 * If any project fails and [stopOnFailure] is true, subsequent tiers are skipped.
 */
class MultiProjectOrchestrator(
  private val executor: EmbeddedTaskExecutor,
) {

  fun interface TaskRunner {
    fun execute(projectName: String, projectPath: String, taskName: String, taskArgs: List<String>, onEvent: (ArchitectEvent<*>) -> Unit): TaskResult
  }

  data class ProjectResult(
    val projectName: String,
    val projectPath: String,
    val result: TaskResult,
    val durationMs: Long,
  )

  data class OrchestrationResult(
    val results: List<ProjectResult>,
    val skipped: List<String>,
  ) {
    val success: Boolean get() = results.all { it.result.success }
    val totalProjects: Int get() = results.size + skipped.size
  }

  /**
   * Runs [taskName] on each project in [targetProjects] in dependency order.
   *
   * @param graph     The full project dependency graph (determines ordering)
   * @param targetProjects Map of project name → project path for projects to execute
   * @param taskName  The task to run on each project
   * @param taskArgs  Arguments to pass to each task invocation
   * @param stopOnFailure When true, stops after the first tier that has any failures
   * @param onEvent   Event listener for streaming output
   * @param onProgress Called before each project starts: (projectName, tier, totalTiers)
   * @param runner    Overrides the default executor (useful for testing)
   */
  fun run(
    graph: ProjectDependencyGraph,
    targetProjects: Map<String, String>,
    taskName: String,
    taskArgs: List<String> = emptyList(),
    stopOnFailure: Boolean = true,
    onEvent: (ArchitectEvent<*>) -> Unit = {},
    onProgress: (projectName: String, tier: Int, totalTiers: Int) -> Unit = { _, _, _ -> },
    runner: TaskRunner = TaskRunner { name, path, task, tArgs, evt -> executor.execute(name, path, task, tArgs, evt) },
  ): OrchestrationResult {
    val tiers = graph.topologicalTiers()
      .map { tier -> tier.filter { it in targetProjects } }
      .filter { it.isNotEmpty() }

    val results = mutableListOf<ProjectResult>()
    val skipped = mutableListOf<String>()
    var aborted = false

    for ((tierIndex, tier) in tiers.withIndex()) {
      if (aborted) continue

      val tierResults = runTierInParallel(
        tier = tier,
        targetProjects = targetProjects,
        taskName = taskName,
        taskArgs = taskArgs,
        onEvent = onEvent,
        onProgress = { name -> onProgress(name, tierIndex + 1, tiers.size) },
        runner = runner,
      )
      results += tierResults

      if (stopOnFailure && tierResults.any { !it.result.success }) {
        aborted = true
      }
    }

    if (aborted) {
      val executedNames = results.map { it.projectName }.toSet()
      skipped += targetProjects.keys.filter { it !in executedNames }
    }

    return OrchestrationResult(results = results, skipped = skipped)
  }

  private fun runTierInParallel(
    tier: List<String>,
    targetProjects: Map<String, String>,
    taskName: String,
    taskArgs: List<String>,
    onEvent: (ArchitectEvent<*>) -> Unit,
    onProgress: (String) -> Unit,
    runner: TaskRunner,
  ): List<ProjectResult> {
    if (tier.size == 1) {
      val name = tier.first()
      val path = targetProjects.getValue(name)
      onProgress(name)
      return listOf(executeProject(name, path, taskName, taskArgs, onEvent, runner))
    }

    val threadPool = Executors.newFixedThreadPool(tier.size.coerceAtMost(MAX_PARALLEL))
    try {
      val futures: List<Future<ProjectResult>> = tier.map { name ->
        val path = targetProjects.getValue(name)
        onProgress(name)
        threadPool.submit(Callable {
          executeProject(name, path, taskName, taskArgs, onEvent, runner)
        })
      }
      return futures.map { it.get(PROJECT_TIMEOUT_MINUTES, TimeUnit.MINUTES) }
    } finally {
      threadPool.shutdown()
    }
  }

  private fun executeProject(
    name: String,
    path: String,
    taskName: String,
    taskArgs: List<String>,
    onEvent: (ArchitectEvent<*>) -> Unit,
    runner: TaskRunner,
  ): ProjectResult {
    val start = System.currentTimeMillis()
    val result = try {
      runner.execute(name, path, taskName, taskArgs, onEvent)
    } catch (e: Exception) {
      TaskResult.failure("Error executing $taskName on $name: ${e.message}")
    }
    return ProjectResult(
      projectName = name,
      projectPath = path,
      result = result,
      durationMs = System.currentTimeMillis() - start,
    )
  }

  companion object {
    private const val MAX_PARALLEL = 8
    private const val PROJECT_TIMEOUT_MINUTES = 30L
  }
}
