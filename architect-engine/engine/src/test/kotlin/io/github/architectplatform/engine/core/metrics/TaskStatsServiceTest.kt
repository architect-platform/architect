package io.github.architectplatform.engine.core.metrics

import io.github.architectplatform.core.history.app.HistoryService
import io.github.architectplatform.core.history.domain.ExecutionRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TaskStatsServiceTest {

  private lateinit var historyService: HistoryService
  private lateinit var service: TaskStatsService

  @BeforeEach
  fun setUp() {
    historyService = mock()
    service = TaskStatsService(historyService)
  }

  private fun record(task: String, durationMs: Long, success: Boolean = true, id: String = java.util.UUID.randomUUID().toString()) =
    ExecutionRecord(
      id = id,
      project = "my-project",
      task = task,
      timestamp = System.currentTimeMillis(),
      success = success,
      durationMs = durationMs,
      message = null,
    )

  // ── getStats ─────────────────────────────────────────────────────────────

  @Test
  fun `getStats returns null when no records match taskId`() {
    whenever(historyService.getByProject(eq("my-project"), any())).thenReturn(emptyList())
    assertNull(service.getStats("my-project", "build"))
  }

  @Test
  fun `getStats ignores records with zero duration`() {
    whenever(historyService.getByProject(eq("my-project"), any()))
      .thenReturn(listOf(record("build", 0), record("build", 0)))
    assertNull(service.getStats("my-project", "build"))
  }

  @Test
  fun `getStats computes correct percentiles for known durations`() {
    // 10 records: 100..1000 ms in steps of 100 ms
    val records = (1..10).map { i -> record("build", i * 100L) }
    whenever(historyService.getByProject(eq("my-project"), any())).thenReturn(records)

    val stats = service.getStats("my-project", "build")
    assertNotNull(stats)
    assertEquals(10, stats!!.sampleCount)
    assertEquals(100L, stats.minDurationMs)
    assertEquals(1000L, stats.maxDurationMs)
    // p50 → index = (0.5 * 10).toInt() = 5 → sorted[5] = 600
    assertEquals(600L, stats.p50DurationMs)
    // p95 → index = (0.95 * 10).toInt() = 9 → sorted[9] = 1000
    assertEquals(1000L, stats.p95DurationMs)
  }

  @Test
  fun `getStats computes correct success rate`() {
    val records = listOf(
      record("build", 500L, success = true),
      record("build", 600L, success = true),
      record("build", 700L, success = false),
      record("build", 800L, success = false),
    )
    whenever(historyService.getByProject(eq("my-project"), any())).thenReturn(records)

    val stats = service.getStats("my-project", "build")!!
    assertEquals(0.5, stats.successRate, 0.001)
  }

  @Test
  fun `getStats returns STABLE trend when fewer than 5 samples`() {
    val records = (1..4).map { i -> record("build", i * 100L) }
    whenever(historyService.getByProject(eq("my-project"), any())).thenReturn(records)

    val stats = service.getStats("my-project", "build")!!
    assertEquals(Trend.STABLE, stats.trend)
  }

  @Test
  fun `getStats returns IMPROVING trend when recent runs are significantly faster`() {
    // Historical (older) runs are slow; recent (first 10) are fast
    val recentFast = (1..10).map { record("build", 100L) }
    val historicalSlow = (1..10).map { record("build", 1000L) }
    whenever(historyService.getByProject(eq("my-project"), any()))
      .thenReturn(recentFast + historicalSlow)

    val stats = service.getStats("my-project", "build")!!
    assertEquals(Trend.IMPROVING, stats.trend)
  }

  @Test
  fun `getStats returns DEGRADING trend when recent runs are significantly slower`() {
    val recentSlow = (1..10).map { record("build", 1000L) }
    val historicalFast = (1..10).map { record("build", 100L) }
    whenever(historyService.getByProject(eq("my-project"), any()))
      .thenReturn(recentSlow + historicalFast)

    val stats = service.getStats("my-project", "build")!!
    assertEquals(Trend.DEGRADING, stats.trend)
  }

  // ── getAllTaskStats ───────────────────────────────────────────────────────

  @Test
  fun `getAllTaskStats groups by task and returns one entry per task`() {
    val records = listOf(
      record("build", 500L),
      record("build", 600L),
      record("test", 300L),
      record("test", 400L),
      record("lint", 100L),
    )
    whenever(historyService.getByProject(eq("my-project"), any())).thenReturn(records)

    val all = service.getAllTaskStats("my-project")
    assertEquals(3, all.size)
    val taskIds = all.map { it.taskId }.toSet()
    assertTrue(taskIds.containsAll(setOf("build", "test", "lint")))
  }

  @Test
  fun `getAllTaskStats returns sorted by taskId`() {
    val records = listOf(
      record("zebra", 500L),
      record("alpha", 300L),
      record("mango", 400L),
    )
    whenever(historyService.getByProject(eq("my-project"), any())).thenReturn(records)

    val all = service.getAllTaskStats("my-project")
    assertEquals(listOf("alpha", "mango", "zebra"), all.map { it.taskId })
  }
}
