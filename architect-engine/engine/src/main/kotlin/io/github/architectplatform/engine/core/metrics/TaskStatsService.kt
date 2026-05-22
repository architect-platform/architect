package io.github.architectplatform.engine.core.metrics

import io.github.architectplatform.core.history.app.HistoryService
import jakarta.inject.Singleton

/**
 * Computes per-task performance statistics from the execution history.
 *
 * Reads [HistoryService] records, filters by project and task, then derives
 * duration percentiles, success rate, and a performance trend.
 */
@Singleton
class TaskStatsService(private val historyService: HistoryService) {

  /**
   * Returns statistics for a single task, or null if no matching records exist.
   *
   * @param project  Project name to filter history
   * @param taskId   Task identifier to match
   * @param limit    Maximum history records to consider (newest first)
   */
  fun getStats(project: String, taskId: String, limit: Int = 100): TaskStats? {
    val records = historyService.getByProject(project, limit)
      .filter { it.task == taskId && it.durationMs > 0 }
    if (records.isEmpty()) return null

    // Durations in ascending order for percentile computation
    val durations = records.map { it.durationMs }.sorted()
    val successRate = records.count { it.success }.toDouble() / records.size

    val avg = durations.average().toLong()
    // "Recent" = last 10 executions (chronological, newest-first in records so .take(10))
    val recentDurations = records.take(RECENT_WINDOW).map { it.durationMs }
    val recentAvg = recentDurations.average().toLong()
    val historicalDurations = records.drop(RECENT_WINDOW).map { it.durationMs }
    val historicalAvg = if (historicalDurations.isNotEmpty()) historicalDurations.average().toLong() else avg

    val trend = when {
      records.size < MIN_RECORDS_FOR_TREND -> Trend.STABLE
      recentAvg < (historicalAvg * IMPROVING_THRESHOLD).toLong() -> Trend.IMPROVING
      recentAvg > (historicalAvg * DEGRADING_THRESHOLD).toLong() -> Trend.DEGRADING
      else -> Trend.STABLE
    }

    return TaskStats(
      taskId = taskId,
      project = project,
      sampleCount = records.size,
      successRate = successRate,
      avgDurationMs = avg,
      minDurationMs = durations.first(),
      maxDurationMs = durations.last(),
      p50DurationMs = percentile(durations, P50),
      p95DurationMs = percentile(durations, P95),
      p99DurationMs = percentile(durations, P99),
      recentAvgMs = recentAvg,
      historicalAvgMs = historicalAvg,
      trend = trend,
    )
  }

  /**
   * Returns statistics for every task that appears in the project's history.
   * Tasks with no successful-duration records are omitted.
   */
  fun getAllTaskStats(project: String, limit: Int = DEFAULT_LIMIT): List<TaskStats> =
    historyService.getByProject(project, limit)
      .groupBy { it.task }
      .mapNotNull { (taskId, _) -> getStats(project, taskId, limit) }
      .sortedBy { it.taskId }

  // ── Internals ────────────────────────────────────────────────────────────

  private fun percentile(sorted: List<Long>, p: Int): Long {
    if (sorted.isEmpty()) return 0L
    val idx = ((p / 100.0) * sorted.size).toInt().coerceIn(0, sorted.size - 1)
    return sorted[idx]
  }
}
