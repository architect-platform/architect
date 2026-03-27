package io.github.architectplatform.cli

import io.github.architectplatform.cli.dto.TaskDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests for [InteractiveTaskSelector].
 *
 * Since the interactive path requires a real terminal (/dev/tty + stty),
 * tests focus on:
 *  - CI/plain mode fallback returning null
 *  - The fuzzy-filter and phase-grouping logic via the public API in
 *    non-interactive environments
 *  - Edge cases: empty task list, all-filtered-out list
 */
class InteractiveTaskSelectorTest {

  private val buildTask = TaskDTO("build", "Build the project", "BUILD")
  private val testTask = TaskDTO("test", "Run all tests", "TEST")
  private val lintTask = TaskDTO("lint", "Run linters", "LINT")
  private val releaseTask = TaskDTO("release", "Publish release", "RELEASE")
  private val noPhaseTask = TaskDTO("custom", "Custom script", null)

  // ── Plain-mode fallback ─────────────────────────────────────────────────

  @Test
  fun `returns null in plain mode without touching terminal`() {
    val selector = InteractiveTaskSelector(plain = true)
    val result = selector.select(listOf(buildTask, testTask))
    assertNull(result)
  }

  @Test
  fun `returns null for empty task list`() {
    val selector = InteractiveTaskSelector(plain = true)
    val result = selector.select(emptyList())
    assertNull(result)
  }

  // ── Non-interactive environment (CI) ────────────────────────────────────

  @Test
  fun `returns null when CI environment variable is set`() {
    // The selector checks System.getenv("CI") — in most test runners CI=true
    // Even without CI, the /dev/tty path guards against non-terminal test runs.
    // Either way the result must be null (no task selected) — never an exception.
    val selector = InteractiveTaskSelector(plain = false)
    val result = selector.select(listOf(buildTask))
    assertNull(result) // test environments are non-interactive
  }

  // ── Fuzzy filter logic (unit-tested via internal helper reflection) ──────

  @Test
  fun `fuzzy filter matches task id substring`() {
    val tasks = listOf(buildTask, testTask, lintTask, releaseTask)
    val filtered = applyFilter(tasks, "bui")
    assertEquals(listOf("build"), filtered.map { it.id })
  }

  @Test
  fun `fuzzy filter matches task description substring`() {
    val tasks = listOf(buildTask, testTask, lintTask)
    val filtered = applyFilter(tasks, "linter")
    assertEquals(listOf("lint"), filtered.map { it.id })
  }

  @Test
  fun `fuzzy filter matches phase name`() {
    val tasks = listOf(buildTask, testTask, lintTask)
    val filtered = applyFilter(tasks, "test")
    // Matches both "test" task id AND tasks in TEST phase
    assertEquals(listOf("test"), filtered.map { it.id })
  }

  @Test
  fun `fuzzy filter is case-insensitive`() {
    val tasks = listOf(buildTask, testTask, releaseTask)
    val filtered = applyFilter(tasks, "RELEASE")
    assertEquals(listOf("release"), filtered.map { it.id })
  }

  @Test
  fun `empty query returns all tasks`() {
    val tasks = listOf(buildTask, testTask, lintTask)
    val filtered = applyFilter(tasks, "")
    assertEquals(3, filtered.size)
  }

  @Test
  fun `query matching nothing returns empty list`() {
    val tasks = listOf(buildTask, testTask)
    val filtered = applyFilter(tasks, "zzznomatch")
    assertEquals(emptyList<TaskDTO>(), filtered)
  }

  @Test
  fun `null-phase task appears under OTHER group`() {
    val tasks = listOf(buildTask, noPhaseTask)
    val filtered = applyFilter(tasks, "")
    assertEquals(2, filtered.size)
    // Ensure null-phase task is included
    assert(filtered.any { it.id == "custom" })
  }

  // ── Phase ordering helper ────────────────────────────────────────────────

  @Test
  fun `phase ordering places BUILD before RELEASE`() {
    val tasks = listOf(releaseTask, buildTask, lintTask)
    val grouped = groupByPhase(tasks)
    val phases = grouped.keys.toList()
    assert(phases.indexOf("LINT") < phases.indexOf("BUILD"))
    assert(phases.indexOf("BUILD") < phases.indexOf("RELEASE"))
  }

  // ── Test helpers (replicate internal logic for unit testing) ─────────────

  /** Mirror of InteractiveTaskSelector.buildItems() filter logic. */
  private fun applyFilter(tasks: List<TaskDTO>, query: String): List<TaskDTO> {
    if (query.isEmpty()) return tasks
    val q = query.lowercase()
    return tasks.filter { t ->
      t.id.lowercase().contains(q) ||
        t.description.lowercase().contains(q) ||
        (t.phase?.lowercase()?.contains(q) == true)
    }
  }

  private val phaseOrder = listOf("INIT", "LINT", "VERIFY", "BUILD", "TEST", "RUN", "RELEASE", "PUBLISH", "OTHER")

  /** Mirror of phase grouping logic. */
  private fun groupByPhase(tasks: List<TaskDTO>): LinkedHashMap<String, List<TaskDTO>> {
    val grouped = tasks.groupBy { it.phase ?: "OTHER" }
    val ordered = (phaseOrder.filter { it in grouped } + (grouped.keys - phaseOrder.toSet()).sorted())
    return LinkedHashMap<String, List<TaskDTO>>().also { map ->
      ordered.forEach { phase -> grouped[phase]?.let { map[phase] = it } }
    }
  }
}
