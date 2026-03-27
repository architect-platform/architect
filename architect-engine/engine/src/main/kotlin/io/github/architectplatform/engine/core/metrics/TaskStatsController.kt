package io.github.architectplatform.engine.core.metrics

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * Exposes per-task performance statistics derived from execution history.
 */
@Controller("/api/projects/{project}/tasks")
@ExecuteOn(TaskExecutors.IO)
class TaskStatsController(private val taskStatsService: TaskStatsService) {

  /**
   * Returns performance statistics for a specific task.
   *
   * @param project Project name
   * @param taskId  Task identifier
   * @param limit   Max history records to consider (default 100)
   * @return 200 with [TaskStats] or 404 if no records found
   */
  @Get("/{taskId}/stats")
  fun getTaskStats(
    @PathVariable project: String,
    @PathVariable taskId: String,
    @QueryValue(defaultValue = "100") limit: Int,
  ): HttpResponse<TaskStats> {
    val stats = taskStatsService.getStats(project, taskId, limit)
      ?: return HttpResponse.notFound()
    return HttpResponse.ok(stats)
  }

  /**
   * Returns performance statistics for all tasks in a project that have history.
   *
   * @param project Project name
   * @param limit   Max history records to consider (default 200)
   */
  @Get("/stats")
  fun getAllTaskStats(
    @PathVariable project: String,
    @QueryValue(defaultValue = "200") limit: Int,
  ): List<TaskStats> = taskStatsService.getAllTaskStats(project, limit)
}
