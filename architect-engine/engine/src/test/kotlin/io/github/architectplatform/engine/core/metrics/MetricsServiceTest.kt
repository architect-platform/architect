package io.github.architectplatform.engine.core.metrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MetricsServiceTest {

  private lateinit var metrics: MetricsService

  @BeforeEach
  fun setUp() {
    metrics = MetricsService()
  }

  @Test
  fun `incrementCounter tracks count`() {
    metrics.incrementCounter("test.counter")
    metrics.incrementCounter("test.counter")
    metrics.incrementCounter("test.counter", 3)
    assertEquals(5, metrics.getCounter("test.counter"))
  }

  @Test
  fun `getCounter returns 0 for unknown counter`() {
    assertEquals(0, metrics.getCounter("unknown"))
  }

  @Test
  fun `recordDuration computes correct stats`() {
    listOf(10L, 20L, 30L, 40L, 50L).forEach { metrics.recordDuration("test.duration", it) }
    val stats = metrics.getDurationStats("test.duration")
    assertEquals(5, stats.count)
    assertEquals(10, stats.min)
    assertEquals(50, stats.max)
    assertEquals(30.0, stats.mean)
    assertEquals(150, stats.sum)
  }

  @Test
  fun `getDurationStats returns EMPTY for unknown`() {
    val stats = metrics.getDurationStats("unknown")
    assertEquals(MetricsService.DurationStats.EMPTY, stats)
  }

  @Test
  fun `toPrometheusFormat includes counters and summaries`() {
    metrics.incrementCounter("architect.tasks.executed", 5)
    metrics.recordDuration("architect.tasks.duration", 100)
    metrics.recordDuration("architect.tasks.duration", 200)
    val output = metrics.toPrometheusFormat()
    assertTrue(output.contains("architect_tasks_executed 5"))
    assertTrue(output.contains("architect_tasks_duration_seconds_count 2"))
    assertTrue(output.contains("quantile=\"0.5\""))
  }

  @Test
  fun `toMap returns structured data`() {
    metrics.incrementCounter("a.counter", 3)
    metrics.recordDuration("a.duration", 42)
    val map = metrics.toMap()
    @Suppress("UNCHECKED_CAST")
    val counters = map["counters"] as Map<String, Long>
    assertEquals(3L, counters["a.counter"])
    assertTrue(map.containsKey("histograms"))
  }

  @Test
  fun `reset clears all data`() {
    metrics.incrementCounter("x", 10)
    metrics.recordDuration("y", 50)
    metrics.reset()
    assertEquals(0, metrics.getCounter("x"))
    assertEquals(MetricsService.DurationStats.EMPTY, metrics.getDurationStats("y"))
  }
}
