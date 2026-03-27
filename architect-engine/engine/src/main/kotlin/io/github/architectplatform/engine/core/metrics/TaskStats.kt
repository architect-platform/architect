package io.github.architectplatform.engine.core.metrics

/**
 * Performance statistics for a single task derived from historical execution records.
 *
 * @property taskId      The task identifier
 * @property project     The project the task belongs to
 * @property sampleCount Number of executions included in these statistics
 * @property successRate Fraction of successful executions in [0..1]
 * @property avgDurationMs   Mean execution duration
 * @property minDurationMs   Minimum observed duration
 * @property maxDurationMs   Maximum observed duration
 * @property p50DurationMs   50th percentile (median)
 * @property p95DurationMs   95th percentile
 * @property p99DurationMs   99th percentile
 * @property recentAvgMs     Average of the most recent executions (last 10)
 * @property historicalAvgMs Average of all older executions (beyond last 10)
 * @property trend           Performance trend based on recent vs historical average
 */
data class TaskStats(
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
  val trend: Trend,
)

/**
 * Direction of performance change compared to historical baseline.
 *
 * Computed by comparing the recent-window average against the historical average:
 * - **IMPROVING**: recent avg < historical avg × 0.90 (≥ 10 % faster)
 * - **DEGRADING**: recent avg > historical avg × 1.10 (≥ 10 % slower)
 * - **STABLE**: within ±10 % of the historical average, or fewer than 5 samples
 */
enum class Trend { IMPROVING, STABLE, DEGRADING }
