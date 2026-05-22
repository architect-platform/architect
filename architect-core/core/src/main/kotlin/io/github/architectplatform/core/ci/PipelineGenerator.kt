package io.github.architectplatform.core.ci

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.core.tasks.infrastructure.InMemoryTaskRegistry

/**
 * Generates a [CiPipeline] from a [TaskRegistry].
 *
 * Maps each real (non-alias, non-synthetic-group) task to a [CiJob], ordering jobs by lifecycle
 * phase. For tasks without explicit dependencies, implicit phase-based ordering is applied: all
 * tasks in a later phase automatically depend on all tasks in the immediately preceding phase.
 */
object PipelineGenerator {

  /**
   * Canonical phase order for pipeline generation.
   * Determines the ordering priority of tasks when building `needs` chains.
   */
  private val PHASE_ORDER: List<String> = listOf(
    // CoreWorkflow phases
    "init", "lint", "verify", "build", "test", "run", "release", "publish",
    // CodeWorkflow phases
    "CODE-init", "CODE-lint", "CODE-verify", "CODE-build", "CODE-test",
    "CODE-run", "CODE-release", "CODE-publish",
    // HooksWorkflow phases
    "pre-commit", "pre-push", "commit-msg",
  )

  /**
   * Phase predecessor map: maps each phase id to the phase that immediately precedes it.
   * Used to inject implicit phase-ordering dependencies.
   */
  private val PHASE_PREDECESSORS: Map<String, String> = mapOf(
    "lint" to "init",
    "verify" to "lint",
    "build" to "verify",
    "test" to "build",
    "run" to "build",
    "release" to "test",
    "publish" to "release",
    "CODE-lint" to "CODE-init",
    "CODE-verify" to "CODE-lint",
    "CODE-build" to "CODE-verify",
    "CODE-test" to "CODE-build",
    "CODE-run" to "CODE-build",
    "CODE-release" to "CODE-test",
    "CODE-publish" to "CODE-release",
    "pre-push" to "pre-commit",
  )

  /**
   * Generates a [CiPipeline] from the given [registry] and project [name].
   *
   * @param registry the task registry populated with project tasks
   * @param name the project name, used as the pipeline name
   * @param trigger the CI trigger configuration
   * @return a fully resolved [CiPipeline]
   */
  fun generate(
    registry: TaskRegistry,
    name: String,
    trigger: CiTrigger = CiTrigger(),
  ): CiPipeline {
    val realTasks = filterRealTasks(registry)
    val jobs = buildJobs(realTasks)
    val orderedJobs = orderJobsByPhase(jobs)
    return CiPipeline(
      name = name,
      on = trigger,
      jobs = orderedJobs,
    )
  }

  /**
   * Filters out alias tasks and synthetic group tasks, returning only real runnable tasks.
   */
  private fun filterRealTasks(registry: TaskRegistry): List<Task> {
    val aliasIds = (registry as? InMemoryTaskRegistry)?.aliasIds().orEmpty()
    val groupIds = registry.groups().keys
    return registry.all().filterNot { it.id in aliasIds || it.id in groupIds }
  }

  /**
   * Builds [CiJob] instances from the list of real tasks.
   * Explicit dependencies from [Task.depends] are preserved; implicit phase-based dependencies
   * will be injected in a subsequent pass.
   */
  private fun buildJobs(tasks: List<Task>): List<CiJob> {
    val taskIdToJobId = tasks.associate { it.id to toJobId(it.id) }
    return tasks.map { task ->
      val explicitNeeds = task.depends().mapNotNull { depId -> taskIdToJobId[depId] }
      CiJob(
        id = toJobId(task.id),
        name = task.description().ifBlank { task.id },
        phase = task.phase()?.id,
        needs = explicitNeeds,
        steps = listOf(
          CiStep(name = "Checkout", uses = "actions/checkout@v4"),
          CiStep(name = "Run ${task.id}", run = "./architect ${task.id}"),
        ),
      )
    }
  }

  /**
   * Orders jobs by their lifecycle phase and injects implicit phase-based `needs` for jobs
   * that have no explicit dependencies.
   *
   * Jobs with an unknown or null phase are placed at the end.
   */
  private fun orderJobsByPhase(jobs: List<CiJob>): List<CiJob> {
    val phaseRank = PHASE_ORDER.withIndex().associate { (index, phase) -> phase to index }

    val sorted = jobs.sortedWith(
      compareBy(
        { phaseRank[it.phase] ?: Int.MAX_VALUE },
        { it.id },
      )
    )

    // Group jobs by phase for implicit ordering lookups
    val jobsByPhase: Map<String?, List<CiJob>> = sorted.groupBy { it.phase }

    return sorted.map { job ->
      if (job.needs.isNotEmpty()) {
        // Explicit dependencies — keep as-is
        job
      } else {
        val predecessorPhase = job.phase?.let { PHASE_PREDECESSORS[it] }
        val implicitNeeds = if (predecessorPhase != null) {
          jobsByPhase[predecessorPhase]?.map { it.id }.orEmpty()
        } else {
          emptyList()
        }
        if (implicitNeeds.isEmpty()) job else job.copy(needs = implicitNeeds)
      }
    }
  }

  /**
   * Converts a task ID into a valid GitHub Actions job ID by replacing characters that are
   * not letters, digits, or hyphens with underscores.
   */
  internal fun toJobId(taskId: String): String =
    taskId.replace(Regex("[^A-Za-z0-9-]"), "_").trimStart('_').ifBlank { "task" }
}
