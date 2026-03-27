package io.github.architectplatform.cli.dto

/**
 * Performance statistics for a task as returned by the engine API.
 */
data class TaskStatsDTO(
  val taskId: String,
  val project: String,
  val sampleCount: Int,
  val successRate: Double,
  val avgDurationMs: Long,
  val minDurationMs: Long,
  val maxDurationMs: Long,
  val p50DurationMs: Long,
  val p95DurationMs: Long,
  val p99DurationMs: Long,
  val recentAvgMs: Long,
  val historicalAvgMs: Long,
  val trend: String,  // IMPROVING | STABLE | DEGRADING
)
