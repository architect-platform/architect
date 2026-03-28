package io.github.architectplatform.cli.embedded

import io.github.architectplatform.cli.dto.TaskDTO
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskNotFoundException
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask
import io.github.architectplatform.core.execution.EmbeddedExecutionContext
import io.github.architectplatform.core.history.domain.ExecutionRecord
import io.github.architectplatform.core.tasks.domain.TaskDependencyResolver
import io.github.architectplatform.core.tasks.infrastructure.InMemoryTaskRegistry
import io.github.architectplatform.core.domain.events.ArchitectEvent
import kotlinx.coroutines.runBlocking
import jakarta.inject.Singleton

@Singleton
class EmbeddedTaskExecutor(
  private val remoteContentFetcher: JdkRemoteContentFetcher,
) {
  var activeProfile: String = "default"
  var outputCacheEnabled: Boolean = false
  private val dependencyResolver = TaskDependencyResolver()

  private fun newContext(): EmbeddedExecutionContext =
    EmbeddedExecutionContext.create(
      remoteContentFetcher = remoteContentFetcher,
      activeProfile = activeProfile,
      outputCacheEnabled = outputCacheEnabled,
    )

  fun listTasks(projectName: String, projectPath: String): List<TaskDTO> {
    val context = newContext()
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: throw IllegalArgumentException("Project $projectName is not registered")
    val registry = project.taskRegistry
    val aliasIds = (registry as? InMemoryTaskRegistry)?.aliasIds().orEmpty()
    val groups = registry.groups()
    val groupIds = groups.keys
    val tasks = registry.all()
      .filterNot { it.id in aliasIds || it.id in groupIds }
    val taskDtos = tasks.associate { task ->
      task.id to TaskDTO(id = task.id, description = task.description(), phase = task.phase()?.id)
    }
    fun resolveGroupMembers(members: List<String>): List<String> {
      val resolved = linkedSetOf<String>()
      members.forEach { memberId ->
        val resolvedTasks = registry.resolve(memberId)
        if (resolvedTasks.isEmpty()) {
          resolved += memberId
        } else {
          resolvedTasks.forEach { resolved += it.id }
        }
      }
      return resolved.toList()
    }

    val groupedMembers = linkedSetOf<String>()
    val ordered = mutableListOf<TaskDTO>()
    groups.forEach { (groupId, members) ->
      val resolvedMembers = resolveGroupMembers(members)
      val groupTask = registry.get(groupId)
      ordered += TaskDTO(
        id = groupId,
        description = groupTask?.description() ?: "Task group '$groupId'",
        phase = groupTask?.phase()?.id,
        groupMembers = resolvedMembers,
      )
      resolvedMembers.forEach { memberId ->
        val member = taskDtos[memberId] ?: return@forEach
        ordered += member
        groupedMembers += memberId
      }
    }
    val ungrouped = taskDtos.values
      .filterNot { it.id in groupedMembers }
      .sortedBy { it.id }
    ordered += ungrouped
    return ordered
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
    val task = resolveTask(projectName, taskName, project.taskRegistry)

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
    val task = resolveTask(projectName, taskName, project.taskRegistry)

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
          timestamp = start,
          success = result.success,
          durationMs = System.currentTimeMillis() - start,
          message = result.message,
          user = System.getProperty("user.name"),
          args = taskArgs,
          result = if (result.success) "SUCCESS" else "FAILURE",
        )
      )
      return result
    } finally {
      unsubscribe()
    }
  }

  private fun resolveTask(projectName: String, taskName: String, registry: TaskRegistry): Task {
    val resolved = registry.resolve(taskName)
    if (resolved.isEmpty()) {
      throw TaskNotFoundException(taskName, projectName, availableTaskReferences(registry))
    }
    return if (resolved.size == 1) {
      resolved.single()
    } else {
      SimpleTask(
        id = taskName,
        description = "Resolved task '$taskName' runs ${resolved.joinToString(", ") { it.id }}",
        customDependencies = resolved.map { it.id },
        permissions = emptySet(),
      ) { _, _ ->
        TaskResult.success("Composite task '$taskName' completed")
      }
    }
  }

  private fun availableTaskReferences(registry: TaskRegistry): List<String> {
    return (registry.all().map { it.id } + registry.groups().keys).distinct()
  }
}
