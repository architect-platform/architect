package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.tasks.Platform
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRequirements

/**
 * Checks the runtime preconditions declared by a task's [TaskRequirements] before execution.
 *
 * This is invoked both:
 * - Eagerly, via `architect check` command (checks all tasks without executing)
 * - Just-in-time, inside [TaskExecutor] before each task runs
 *
 * Checks performed:
 * 1. **Tools** — each declared tool must be present on PATH (`which <tool>` / `where <tool>`)
 * 2. **Minimum tool versions** — when declared, the installed version must be ≥ minimum
 * 3. **Environment variables** — each declared env var must be set and non-empty
 * 4. **Platform** — when declared, the current OS must be in the allowed set
 *
 * Each unsatisfied requirement produces a [ConditionIssue] with an actionable [ConditionIssue.hint]
 * that tells the user exactly what to do to fix it.
 */
class TaskConditionChecker {

    /**
     * Checks all requirements for a single [task].
     *
     * @return A [ConditionCheckResult] summarising which conditions passed or failed
     */
    fun check(task: Task): ConditionCheckResult {
        val requirements = task.requires() ?: return ConditionCheckResult(task.id, satisfied = true, issues = emptyList())
        return check(task.id, requirements)
    }

    /**
     * Checks explicit [requirements] for a given [taskId].
     * Useful when the caller already has the requirements object.
     */
    fun check(taskId: String, requirements: TaskRequirements): ConditionCheckResult {
        val issues = mutableListOf<ConditionIssue>()

        // 1. Platform check
        if (requirements.platform.isNotEmpty()) {
            val current = Platform.current()
            if (current !in requirements.platform) {
                val allowed = requirements.platform.joinToString(", ") { it.name.lowercase() }
                issues += ConditionIssue(
                    kind = IssueKind.PLATFORM,
                    message = "Task '$taskId' requires platform [$allowed] but running on ${current.name.lowercase()}",
                    hint = "Run this task on one of: $allowed",
                )
            }
        }

        // 2. Environment variable checks
        for (envVar in requirements.env) {
            val value = System.getenv(envVar)
            if (value.isNullOrBlank()) {
                issues += ConditionIssue(
                    kind = IssueKind.ENV,
                    message = "Required environment variable '$envVar' is not set",
                    hint = "export $envVar=<value>",
                )
            }
        }

        // 3. Tool availability checks
        for (tool in requirements.tools) {
            if (!isToolAvailable(tool)) {
                issues += ConditionIssue(
                    kind = IssueKind.TOOL_MISSING,
                    message = "Required tool '$tool' was not found on PATH",
                    hint = "Install '$tool' and ensure it is on your PATH",
                )
                // Skip version check — tool is not present
                continue
            }

            // 4. Minimum version check
            val minVersion = requirements.minToolVersions[tool]
            if (minVersion != null) {
                val installed = getToolVersion(tool)
                if (installed != null && !isVersionSufficient(installed, minVersion)) {
                    issues += ConditionIssue(
                        kind = IssueKind.TOOL_VERSION,
                        message = "Tool '$tool' version $installed is below minimum required $minVersion",
                        hint = "Upgrade '$tool' to version $minVersion or later",
                    )
                }
            }
        }

        return ConditionCheckResult(
            taskId = taskId,
            satisfied = issues.isEmpty(),
            issues = issues,
        )
    }

    /**
     * Checks requirements for all tasks in the [tasks] list.
     */
    fun checkAll(tasks: List<Task>): List<ConditionCheckResult> = tasks.map { check(it) }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    internal fun isToolAvailable(tool: String): Boolean {
        val os = System.getProperty("os.name").lowercase()
        val command = if (os.contains("win")) listOf("where", tool) else listOf("which", tool)
        return try {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    internal fun getToolVersion(tool: String): String? {
        for (flag in listOf("--version", "version", "-v", "-V")) {
            try {
                val proc = ProcessBuilder(tool, flag)
                    .redirectErrorStream(true)
                    .start()
                val output = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                val version = extractVersion(output)
                if (version != null) return version
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Extracts the first `MAJOR.MINOR.PATCH` (or `MAJOR.MINOR`) version string from [text].
     */
    internal fun extractVersion(text: String): String? {
        val pattern = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""")
        return pattern.find(text)?.value
    }

    /**
     * Returns true if [installed] ≥ [minimum] using numeric component-by-component comparison.
     */
    internal fun isVersionSufficient(installed: String, minimum: String): Boolean {
        val ins = installed.split(".").mapNotNull { it.toIntOrNull() }
        val min = minimum.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(ins.size, min.size)
        for (i in 0 until len) {
            val a = ins.getOrElse(i) { 0 }
            val b = min.getOrElse(i) { 0 }
            if (a > b) return true
            if (a < b) return false
        }
        return true // equal
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Result types
// ──────────────────────────────────────────────────────────────────────────

/**
 * The outcome of checking one task's requirements.
 */
data class ConditionCheckResult(
    val taskId: String,
    val satisfied: Boolean,
    val issues: List<ConditionIssue>,
)

/**
 * A single unsatisfied requirement.
 *
 * @property kind Category of the issue
 * @property message Developer-facing description of what is wrong
 * @property hint Actionable instruction to fix the issue
 */
data class ConditionIssue(
    val kind: IssueKind,
    val message: String,
    val hint: String,
)

/**
 * Category of a [ConditionIssue].
 */
enum class IssueKind {
    TOOL_MISSING,
    TOOL_VERSION,
    ENV,
    PLATFORM,
}
