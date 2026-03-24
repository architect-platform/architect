package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.tasks.Platform
import io.github.architectplatform.api.core.tasks.TaskRequirements
import io.github.architectplatform.api.core.tasks.impl.SimpleTask
import io.github.architectplatform.api.core.tasks.TaskResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskConditionCheckerTest {

    private val checker = TaskConditionChecker()

    // ──────────────────────────────────────────────────────────────────────
    // Tasks without requirements
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `task without requires returns satisfied result`() {
        val task = simpleTask("no-reqs")
        val result = checker.check(task)
        assertTrue(result.satisfied)
        assertTrue(result.issues.isEmpty())
        assertEquals("no-reqs", result.taskId)
    }

    @Test
    fun `task with empty requirements returns satisfied result`() {
        val task = simpleTask("empty-reqs", TaskRequirements())
        val result = checker.check(task)
        assertTrue(result.satisfied)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool availability
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `check passes for tool that exists on PATH`() {
        // 'sh' is universally available on Unix-like systems
        val reqs = TaskRequirements(tools = listOf("sh"))
        val result = checker.check("test-task", reqs)
        assertTrue(result.satisfied)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `check fails for tool that does not exist`() {
        val reqs = TaskRequirements(tools = listOf("__nonexistent_tool_xyz_architect__"))
        val result = checker.check("test-task", reqs)
        assertFalse(result.satisfied)
        assertEquals(1, result.issues.size)
        assertEquals(IssueKind.TOOL_MISSING, result.issues[0].kind)
        assertTrue(result.issues[0].message.contains("__nonexistent_tool_xyz_architect__"))
        assertTrue(result.issues[0].hint.isNotBlank())
    }

    @Test
    fun `multiple missing tools produce separate issues`() {
        val reqs = TaskRequirements(tools = listOf("__tool_a__", "__tool_b__"))
        val result = checker.check("test-task", reqs)
        assertFalse(result.satisfied)
        assertEquals(2, result.issues.size)
        assertTrue(result.issues.all { it.kind == IssueKind.TOOL_MISSING })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Environment variables
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `check passes when required env var is set`() {
        val varName = "PATH" // Always present
        val reqs = TaskRequirements(env = listOf(varName))
        val result = checker.check("test-task", reqs)
        assertTrue(result.satisfied, "Expected satisfaction for env var PATH, got: ${result.issues}")
    }

    @Test
    fun `check fails when required env var is not set`() {
        val reqs = TaskRequirements(env = listOf("__ARCHITECT_TEST_ABSENT_VAR__"))
        val result = checker.check("test-task", reqs)
        assertFalse(result.satisfied)
        assertEquals(1, result.issues.size)
        assertEquals(IssueKind.ENV, result.issues[0].kind)
        assertTrue(result.issues[0].hint.contains("__ARCHITECT_TEST_ABSENT_VAR__"))
    }

    // ──────────────────────────────────────────────────────────────────────
    // Platform
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @EnabledOnOs(OS.LINUX)
    fun `check passes when current platform is in allowed set`() {
        val reqs = TaskRequirements(platform = setOf(Platform.LINUX))
        val result = checker.check("test-task", reqs)
        assertTrue(result.satisfied)
    }

    @Test
    @EnabledOnOs(OS.MAC)
    fun `check passes on darwin when darwin is allowed`() {
        val reqs = TaskRequirements(platform = setOf(Platform.DARWIN))
        val result = checker.check("test-task", reqs)
        assertTrue(result.satisfied)
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun `check fails when current platform is not in allowed set`() {
        val reqs = TaskRequirements(platform = setOf(Platform.WINDOWS))
        val result = checker.check("test-task", reqs)
        assertFalse(result.satisfied)
        assertEquals(1, result.issues.size)
        assertEquals(IssueKind.PLATFORM, result.issues[0].kind)
    }

    @Test
    fun `empty platform set allows any platform`() {
        val reqs = TaskRequirements(platform = emptySet())
        val result = checker.check("test-task", reqs)
        // Platform issues only appear when the set is non-empty and current is not in it
        val platformIssues = result.issues.filter { it.kind == IssueKind.PLATFORM }
        assertTrue(platformIssues.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────
    // Version extraction and comparison
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `extractVersion returns first semver string`() {
        val version = checker.extractVersion("git version 2.42.1")
        assertEquals("2.42.1", version)
    }

    @Test
    fun `extractVersion handles multi-word output`() {
        val version = checker.extractVersion("Docker version 24.0.5, build ced0996")
        assertEquals("24.0.5", version)
    }

    @Test
    fun `extractVersion returns null when no version found`() {
        val version = checker.extractVersion("no version here")
        assertNull(version)
    }

    @Test
    fun `isVersionSufficient returns true when installed equals minimum`() {
        assertTrue(checker.isVersionSufficient("2.0.0", "2.0.0"))
    }

    @Test
    fun `isVersionSufficient returns true when installed is higher`() {
        assertTrue(checker.isVersionSufficient("3.1.0", "2.0.0"))
        assertTrue(checker.isVersionSufficient("2.1.0", "2.0.9"))
    }

    @Test
    fun `isVersionSufficient returns false when installed is lower`() {
        assertFalse(checker.isVersionSufficient("1.9.9", "2.0.0"))
        assertFalse(checker.isVersionSufficient("20.0.0", "20.1.0"))
    }

    @Test
    fun `isVersionSufficient handles two-part versions`() {
        assertTrue(checker.isVersionSufficient("3.0", "2.9"))
        assertFalse(checker.isVersionSufficient("2.0", "3.0"))
    }

    // ──────────────────────────────────────────────────────────────────────
    // checkAll
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAll returns one result per task`() {
        val tasks = listOf(simpleTask("a"), simpleTask("b"), simpleTask("c"))
        val results = checker.checkAll(tasks)
        assertEquals(3, results.size)
        assertEquals(listOf("a", "b", "c"), results.map { it.taskId })
    }

    @Test
    fun `checkAll returns satisfied for all tasks with no requirements`() {
        val tasks = (1..5).map { simpleTask("task-$it") }
        assertTrue(checker.checkAll(tasks).all { it.satisfied })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun simpleTask(id: String, requirements: TaskRequirements? = null) =
        SimpleTask(
            id = id,
            description = "Test task $id",
            requirements = requirements,
            task = { _, _ -> TaskResult.success() },
        )
}
