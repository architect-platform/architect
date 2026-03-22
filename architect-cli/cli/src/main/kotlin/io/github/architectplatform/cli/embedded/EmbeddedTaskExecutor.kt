package io.github.architectplatform.cli.embedded

import io.github.architectplatform.cli.dto.TaskDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.execution.EmbeddedExecutionContext
import io.github.architectplatform.engine.core.history.domain.ExecutionRecord
import io.github.architectplatform.engine.core.tasks.domain.TaskDependencyResolver
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import kotlinx.coroutines.runBlocking
import jakarta.inject.Singleton

@Singleton
class EmbeddedTaskExecutor(
  private val remoteContentFetcher: JdkRemoteContentFetcher,
  var activeProfile: String = "default",
) {
  private val dependencyResolver = TaskDependencyResolver()

  private fun newContext(): EmbeddedExecutionContext =
    EmbeddedExecutionContext.create(
      remoteContentFetcher = remoteContentFetcher,
      activeProfile = activeProfile,
    )

  fun listTasks(projectName: String, projectPath: String): List<TaskDTO> {
    val context = newContext()
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: throw IllegalArgumentException("Project $projectName is not registered")
    return project.taskRegistry.all()
        .sortedBy { it.id }
        .map { TaskDTO(id = it.id, description = it.description(), phase = it.phase()?.id) }
  }

  fun validate(projectName: String, projectPath: String): ValidationResultDTO {
    val context = newContext()
    context.projectService.registerProject(projectName, projectPath)
    val result = context.projectService.validateProject(projectName)
    return ValidationResultDTO(result.valid, result.errors, result.warnings)
  }

  fun plan(projectName: String, projectPath: String, taskName: String): TaskPlanDTO {
    val context = newContext()
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: throw IllegalArgumentException("Project $projectName is not registered")
    val task = project.taskRegistry.get(taskName)
      ?: throw IllegalArgumentException("Task '$taskName' not found in project '$projectName'")

    val allTasks = dependencyResolver.resolveAllDependencies(task, project.taskRegistry)
    val executionOrder = dependencyResolver.topologicalSort(allTasks)
    val batches = dependencyResolver.toBatches(executionOrder)
    val steps = executionOrder.map { t ->
      TaskPlanStepDTO(
        id = t.id,
        description = t.description(),
        phase = t.phase()?.id,
        depends = t.depends(),
        batch = batches[t.id] ?: 0,
      )
    }

    return TaskPlanDTO(
      task = taskName,
      project = projectName,
      steps = steps,
      totalSteps = steps.size,
      parallelBatches = steps.map { it.batch }.distinct().size,
    )
  }

  fun execute(
    projectName: String,
    projectPath: String,
    taskName: String,
    taskArgs: List<String>,
    onEvent: (ArchitectEvent<*>) -> Unit,
  ): TaskResult {
    val context = newContext()
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: throw IllegalArgumentException("Project $projectName is not registered")
    val task = project.taskRegistry.get(taskName)
      ?: throw IllegalArgumentException("Task '$taskName' not found in project '$projectName'")

    val unsubscribe = context.eventBus.subscribe(onEvent)

    val start = System.currentTimeMillis()
    try {
      val (executionId, deferred) =
        context.taskExecutor.execute(project, task, project.context, taskArgs)
      val result = runBlocking { deferred.await() }
      context.historyService.record(
        ExecutionRecord(
          id = executionId,
          project = projectName,
          task = taskName,
          timestamp = System.currentTimeMillis(),
          success = result.success,
          durationMs = System.currentTimeMillis() - start,
          message = result.message,
        )
      )
      return result
    } finally {
      unsubscribe()
    }
  }
}
