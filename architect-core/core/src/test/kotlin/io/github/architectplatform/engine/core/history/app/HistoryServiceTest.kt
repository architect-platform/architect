package io.github.architectplatform.engine.core.history.app

import io.github.architectplatform.engine.core.history.domain.ExecutionRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Tests for [HistoryService] — write, read all (sorted), read by project, limit,
 * directory creation, and JSON round-trip.
 */
class HistoryServiceTest {

  private lateinit var historyDir: File
  private lateinit var service: HistoryService
  private lateinit var originalHome: String

  @BeforeEach
  fun setUp(@TempDir tmpDir: Path) {
    originalHome = System.getProperty("user.home")
    val home = tmpDir.toAbsolutePath().toString()
    System.setProperty("user.home", home)
    historyDir = File(home, ".architect/history")
    service = HistoryService()
  }

  @AfterEach
  fun tearDown() {
    System.setProperty("user.home", originalHome)
  }

  @Test
  fun `record and retrieve a single record`() {
    val record = record("exec-1", "api", "build", success = true)

    service.record(record)

    val records = service.getAll()
    assertEquals(1, records.size)
    assertEquals("exec-1", records[0].id)
  }

  @Test
  fun `getAll returns records sorted newest first`() {
    service.record(record("a", "p1", "build", timestamp = 1000L, success = true))
    service.record(record("b", "p1", "test", timestamp = 2000L, success = true))
    service.record(record("c", "p1", "deploy", timestamp = 3000L, success = true))

    val all = service.getAll()

    assertEquals(3, all.size)
    // Newest first (by filename sort descending)
    // The files are named with timestamps, so the one recorded last should appear first
    assertEquals("c", all[0].id)
    assertEquals("b", all[1].id)
    assertEquals("a", all[2].id)
  }

  @Test
  fun `getAll respects limit`() {
    repeat(10) { i ->
      service.record(record("exec-$i", "proj", "task-$i", timestamp = i * 1000L, success = true))
    }

    val limited = service.getAll(limit = 3)

    assertEquals(3, limited.size)
  }

  @Test
  fun `getByProject filters by project name`() {
    service.record(record("a", "api", "build", success = true))
    service.record(record("b", "web", "lint", success = true))
    service.record(record("c", "api", "test", success = false))

    val apiRecords = service.getByProject("api")

    assertEquals(2, apiRecords.size)
    assertTrue(apiRecords.all { it.project == "api" })
  }

  @Test
  fun `JSON round-trip preserves all fields`() {
    val original = ExecutionRecord(
      id = "round-trip-1",
      project = "my-project",
      task = "full-build",
      timestamp = 1711100000000L,
      success = false,
      durationMs = 4567L,
      message = "Build failed: missing dependency",
    )

    service.record(original)
    val records = service.getAll()

    assertEquals(1, records.size)
    val loaded = records[0]
    assertEquals(original.id, loaded.id)
    assertEquals(original.project, loaded.project)
    assertEquals(original.task, loaded.task)
    assertEquals(original.timestamp, loaded.timestamp)
    assertEquals(original.success, loaded.success)
    assertEquals(original.durationMs, loaded.durationMs)
    assertEquals(original.message, loaded.message)
  }

  @Test
  fun `getAll returns empty list when no records exist`() {
    val records = service.getAll()

    assertTrue(records.isEmpty())
  }

  @Test
  fun `getByProject returns empty when no matching project`() {
    service.record(record("a", "api", "build", success = true))

    val records = service.getByProject("nonexistent")

    assertTrue(records.isEmpty())
  }

  // ─── helpers ──────────────────────────────────────────────────────────────

  private fun record(
    id: String,
    project: String,
    task: String,
    timestamp: Long = System.currentTimeMillis(),
    success: Boolean,
  ) = ExecutionRecord(
    id = id,
    project = project,
    task = task,
    timestamp = timestamp,
    success = success,
    durationMs = 100L,
    message = if (success) "ok" else "failed",
  )
}
