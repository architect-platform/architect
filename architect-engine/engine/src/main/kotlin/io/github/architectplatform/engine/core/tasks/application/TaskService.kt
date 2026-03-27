package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskNotFoundException
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.audit.AuditService
import io.github.architectplatform.engine.cloud.CloudReporterService
import io.github.architectplatform.core.history.app.HistoryService
import io.github.architectplatform.core.history.domain.ExecutionRecord
import io.github.architectplatform.core.project.app.ProjectService
import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.tasks.domain.TaskDependencyResolver
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionCancelledEvent
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionCompletedEvent
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionFailedEvent
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionStartedEvent
import io.github.architectplatform.core.tasks.dto.TaskPlanDTO
import io.github.architectplatform.core.tasks.dto.TaskPlanStepDTO
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.ExecutionId
import io.github.architectplatform.core.domain.events.generateExecutionId
import io.github.architectplatform.core.tasks.application.TaskExecutor
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

/**
 * Service responsible for task management and execution within projects.
 *
 * This service provides functionality to:
 * - Discover and retrieve tasks available in registered projects
 * - Execute tasks with custom arguments
 * - Stream execution events in real-time
 * - Handle recursive task execution across project hierarchies
 *
 * @property projectService Service for project management
 * @property executor Executor for running tasks
 * @property eventCollector Collector for execution event streams
 */
@Singleton
class TaskService(
    private val projectService: ProjectService,
    private val executor: TaskExecutor,
    private val eventCollector: ExecutionEventCollector,
    private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>,
    private val historyService: HistoryService,
    private val metricsService: io.github.architectplatform.engine.core.metrics.MetricsService,
    private val auditService: AuditService,
    private val cloudReporter: Optional<CloudReporterService> = Optional.empty(),
    @Property(
        name = io.github.architectplatform.core.config.EngineConfiguration.TaskExecution.EXECUTION_TIMEOUT_SECONDS,
        defaultValue = "${io.github.architectplatform.core.config.EngineConfiguration.TaskExecution.DEFAULT_EXECUTION_TIMEOUT_SECONDS}"
    )
    private val executionTimeoutSeconds: Long = io.github.architectplatform.core.config.EngineConfiguration.TaskExecution.DEFAULT_EXECUTION_TIMEOUT_SECONDS,
) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this::class.java)
  private val runningJobs = java.util.concurrent.ConcurrentHashMap<ExecutionId, Job>()
  private val executionProjects = java.util.concurrent.ConcurrentHashMap<ExecutionId, String>()

  /**
   * Retrieves all available tasks for a project.
   *
   * @param projectName The name of the project
   * @return List of tasks sorted by their identifiers
   * @throws IllegalArgumentException if the project is not found
   */
  fun getAllTasks(projectName: String): List<Task> {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")
    return project.taskRegistry.all().sortedBy { it.id }
  }

  /**
   * Retrieves a specific task by its identifier.
   *
   * @param projectName The name of the project
   * @param taskId The unique identifier of the task
   * @return The task instance
   * @throws IllegalArgumentException if the project or task is not found
   */
  fun getTaskById(projectName: String, taskId: String): Task {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")
    return project.taskRegistry.get(taskId)
      ?: throw TaskNotFoundException(taskId, projectName, project.taskRegistry.all().map { it.id })
  }

  /**
   * Computes the execution plan for a task without running it.
   *
   * Returns the ordered list of tasks that would execute, including transitive dependencies,
   * with each task assigned a parallel batch index (tasks in the same batch have no ordering
   * dependency on each other and can run concurrently).
   *
   * @param projectName The name of the project
   * @param taskId The unique identifier of the task
   * @return The execution plan with ordered steps and batch assignments
   * @throws IllegalArgumentException if the project or task is not found
   */
  fun planTask(projectName: String, taskId: String): TaskPlanDTO {
    val project = projectService.getProject(projectName)
        ?: throw IllegalArgumentException("Project not found")
    val task = project.taskRegistry.get(taskId)
        ?: throw TaskNotFoundException(taskId, projectName, project.taskRegistry.all().map { it.id })

    val resolver = TaskDependencyResolver()
    val allTasks = resolver.resolveAllDependencies(task, project.taskRegistry)
    val ordered = resolver.topologicalSort(allTasks)
    val batches = resolver.toBatches(ordered)

    val steps = ordered.map { t ->
      TaskPlanStepDTO(
          id = t.id,
          description = t.description(),
          phase = t.phase()?.id,
          depends = t.depends(),
          batch = batches[t.id] ?: 0,
      )
    }
    return TaskPlanDTO(task = taskId, project = projectName, steps = steps)
  }

  /**
   * Executes a task within a project hierarchy.
   *
   * The task is executed recursively across all subprojects first (depth-first),
   * then executed in the parent project. This ensures proper dependency ordering
   * in multi-project builds. All subprojects share the same executionId for unified
   * event tracking.
   *
   * @param projectName The name of the project
   * @param taskId The unique identifier of the task to execute
   * @param args List of arguments to pass to the task
   * @return The execution ID for tracking the task execution
   * @throws IllegalArgumentException if the project or task is not found
   */
  fun executeTask(projectName: String, taskId: String, args: List<String>): ExecutionId {
    val project =
        projectService.getProject(projectName)
            ?: throw IllegalArgumentException("Project not found")

    // Generate a single execution ID for the entire execution tree
    val executionId = generateExecutionId()
      executionProjects[executionId] = projectName
      val job = CoroutineScope(IO).launch {
          val startTime = System.currentTimeMillis()
          eventPublisher.publishEvent(
              executionStartedEvent(
                  projectName,
                  executionId,
                  message = "Starting execution of task: $taskId in project: $projectName")
          )
        val result = if (executionTimeoutSeconds > 0) {
          val timeoutMs = executionTimeoutSeconds * 1000L
          withTimeoutOrNull(timeoutMs) {
            executeRecursivelyOverSubprojectsFirst(project, taskId, args, executionId = executionId)
          } ?: run {
            logger.warn("Execution $executionId timed out after ${executionTimeoutSeconds}s")
            eventPublisher.publishEvent(
              executionCancelledEvent(
                projectName, executionId,
                message = "Execution timed out after ${executionTimeoutSeconds}s"
              )
            )
            TaskResult.failure("Execution timed out after ${executionTimeoutSeconds}s")
          }
        } else {
          executeRecursivelyOverSubprojectsFirst(project, taskId, args, executionId = executionId)
        }
        val durationMs = System.currentTimeMillis() - startTime
        val record = ExecutionRecord(
            id = executionId,
            project = projectName,
            task = taskId,
            timestamp = startTime,
            success = result.success,
            durationMs = durationMs,
            message = result.message,
            user = System.getProperty("user.name"),
            args = args,
            result = if (result.success) "SUCCESS" else "FAILURE",
        )
        historyService.record(record)
        cloudReporter.ifPresent { reporter -> reporter.reportAuditRecord(record) }
        metricsService.incrementCounter("architect.tasks.executed")
        metricsService.recordDuration("architect.tasks.duration", durationMs)
        if (result.success) {
          metricsService.incrementCounter("architect.tasks.succeeded")
        } else {
          metricsService.incrementCounter("architect.tasks.failed")
        }
        auditService.record(
            projectName = projectName,
            taskName = taskId,
            status = if (result.success) "SUCCESS" else "FAILURE",
            durationMs = durationMs,
            executionId = executionId,
            message = result.message,
        )
        if (!result.success) {
            eventPublisher.publishEvent(
                executionFailedEvent(
                    projectName,
                    executionId,
                    message = "Execution failed: $result.",
                    errorDetails = "${result.message}",
                )
            )
        } else {
            eventPublisher.publishEvent(
                executionCompletedEvent(
                    projectName,
                    executionId,
                    message = "All tasks completed successfully")
            )
        }
        runningJobs.remove(executionId)
        executionProjects.remove(executionId)
      }
      runningJobs[executionId] = job

    return executionId
  }

    private suspend fun executeRecursivelyOverSubprojectsFirst(
        project: Project,
        taskId: String,
        args: List<String>,
        parentProject: String? = null,
        executionId: ExecutionId = generateExecutionId(),
    ): TaskResult {
        // Execute all subprojects concurrently and wait for results
        val subResults = project.subProjects.map { subProject ->
            CoroutineScope(IO).async {
                executeRecursivelyOverSubprojectsFirst(subProject, taskId, args, project.name, executionId)
            }
        }.awaitAll()

        // If any subproject failed, propagate failure
        if (subResults.any { !it.success }) {
            return TaskResult.failure("Some subprojects failed", subResults)
        }

        // Execute task in the current project only if present
        val task = project.taskRegistry.all().firstOrNull { it.id == taskId }
        if (task == null) {
            return TaskResult.skipped("Task '$taskId' not found in project '${project.name}', skipping.")
        }

        // Execute using shared executionId
        val (_, deferredResult) =
            executor.execute(project, task, project.context, args, parentProject, executionId)

        return deferredResult.await()
    }

  /**
   * Cancels a running execution by its ID.
   *
   * @param executionId The execution ID to cancel
   * @return true if the execution was found and cancelled, false if not found
   */
  fun cancelExecution(executionId: ExecutionId): Boolean {
    val job = runningJobs.remove(executionId) ?: return false
    val projectName = executionProjects.remove(executionId) ?: "unknown"
    job.cancel()
    eventPublisher.publishEvent(
      executionCancelledEvent(
        projectName,
        executionId,
        message = "Execution cancelled by user"
      )
    )
    return true
  }

  private fun generateExecutionId(): ExecutionId = UUID.randomUUID().toString()

  /**
   * Returns a flow of execution events for a specific task execution.
   *
   * This provides real-time streaming of events such as task start, progress,
   * completion, and failures during task execution.
   *
   * @param executionId The unique identifier of the execution
   * @return Flow of execution events
   */
  fun getExecutionFlow(executionId: ExecutionId): Flow<ArchitectEvent<ExecutionEvent>> {
    return eventCollector.getFlow(executionId)
  }
}
