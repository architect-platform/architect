package io.github.architectplatform.engine.core.metrics

import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight in-process metrics collector for task execution.
 *
 * Tracks counters (total, success, failure, skipped, cancelled, cache hits)
 * and histograms (task duration distribution).
 */
@Singleton
class MetricsService {

  private val counters = ConcurrentHashMap<String, AtomicLong>()
  private val durations = ConcurrentHashMap<String, MutableList<Long>>()

  fun incrementCounter(name: String, amount: Long = 1) {
    counters.getOrPut(name) { AtomicLong(0) }.addAndGet(amount)
  }

  fun recordDuration(name: String, durationMs: Long) {
    durations.getOrPut(name) { java.util.Collections.synchronizedList(mutableListOf()) }
      .add(durationMs)
  }

  fun getCounter(name: String): Long = counters[name]?.get() ?: 0

  fun getDurationStats(name: String): DurationStats {
    val values = durations[name]?.toList() ?: return DurationStats.EMPTY
    if (values.isEmpty()) return DurationStats.EMPTY
    val sorted = values.sorted()
    return DurationStats(
      count = sorted.size.toLong(),
      min = sorted.first(),
      max = sorted.last(),
      mean = sorted.average(),
      p50 = percentile(sorted, 0.50),
      p90 = percentile(sorted, 0.90),
      p99 = percentile(sorted, 0.99),
      sum = sorted.sum(),
    )
  }

  /**
   * Returns all metrics in Prometheus text exposition format.
   */
  fun toPrometheusFormat(): String {
    val sb = StringBuilder()

    // Counters
    for ((name, value) in counters.toSortedMap()) {
      val promName = name.replace(".", "_")
      sb.appendLine("# TYPE $promName counter")
      sb.appendLine("$promName ${value.get()}")
    }

    // Histograms
    for ((name, _) in durations.toSortedMap()) {
      val stats = getDurationStats(name)
      if (stats.count == 0L) continue
      val promName = name.replace(".", "_")
      sb.appendLine("# TYPE ${promName}_seconds summary")
      sb.appendLine("${promName}_seconds_count ${stats.count}")
      sb.appendLine("${promName}_seconds_sum ${"%.3f".format(stats.sum / 1000.0)}")
      sb.appendLine("${promName}_seconds{quantile=\"0.5\"} ${"%.3f".format(stats.p50 / 1000.0)}")
      sb.appendLine("${promName}_seconds{quantile=\"0.9\"} ${"%.3f".format(stats.p90 / 1000.0)}")
      sb.appendLine("${promName}_seconds{quantile=\"0.99\"} ${"%.3f".format(stats.p99 / 1000.0)}")
    }

    return sb.toString()
  }

  /**
   * Returns all metrics as a structured map for JSON endpoints.
   */
  fun toMap(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    result["counters"] = counters.mapValues { it.value.get() }.toSortedMap()
    result["histograms"] = durations.keys.sorted().associateWith { getDurationStats(it) }
    return result
  }

  fun reset() {
    counters.clear()
    durations.clear()
  }

  private fun percentile(sorted: List<Long>, p: Double): Long {
    val index = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
    return sorted[index]
  }

  data class DurationStats(
    val count: Long,
    val min: Long,
    val max: Long,
    val mean: Double,
    val p50: Long,
    val p90: Long,
    val p99: Long,
    val sum: Long,
  ) {
    companion object {
      val EMPTY = DurationStats(0, 0, 0, 0.0, 0, 0, 0, 0)
    }
  }
}
