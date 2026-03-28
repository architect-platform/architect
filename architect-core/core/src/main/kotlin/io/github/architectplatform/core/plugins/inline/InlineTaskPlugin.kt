package io.github.architectplatform.core.plugins.inline

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.components.workflows.hooks.HooksWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.FailureStrategy
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskRequirements
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.tasks.Platform
import io.github.architectplatform.core.plugins.inline.InlineTaskConfig
import java.time.Duration

/**
 * Built-in engine plugin that reads the `tasks:` section of `architect.yml` and registers
 * each entry as a [SimpleTask] with shell execution.
 *
 * This allows projects to define lightweight tasks directly in configuration without
 * writing a plugin. The `tasks:` section is a YAML map where each key is the task ID:
 *
 * ```yaml
 * tasks:
 *   deploy:
 *     description: "Deploy to staging"
 *     phase: PUBLISH
 *     depends: [build, test]
 *     run: "kubectl apply -f k8s/"
 *   smoke-test:
 *     description: "Run smoke tests"
 *     run: "curl -f https://staging.example.com/health"
 * ```
 *
 * Supported phases: all CoreWorkflow, CodeWorkflow, and HooksWorkflow values.
 * If `run` is empty or missing, the task is skipped.
 */
@Suppress("UNCHECKED_CAST")
class InlineTaskPlugin : ArchitectPlugin<HashMap<String, Any>> {

    override val id: String = "inline-tasks"
    override val contextKey: String = "tasks"
    override val ctxClass: Class<HashMap<String, Any>> = HashMap::class.java as Class<HashMap<String, Any>>
    override var context: HashMap<String, Any> = HashMap()

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun init(context: Any) {
        if (context is Map<*, *>) {
            this.context = HashMap(context as Map<String, Any>)
        }
    }

    override fun register(registry: TaskRegistry) {
        for ((taskId, rawConfig) in context) {
            val config = try {
                objectMapper.convertValue(rawConfig, InlineTaskConfig::class.java)
            } catch (e: Exception) {
                continue
            }
            if (config.run.isBlank()) continue

            val phase = config.phase?.let { resolvePhase(it) }
            val command = config.run
            val permissions = try {
                TaskPermission.fromWireNames(config.permissions)
            } catch (_: IllegalArgumentException) {
                continue
            }

            val requirements = config.requires?.let { req ->
                TaskRequirements(
                    tools = req.tools,
                    minToolVersions = req.minToolVersions,
                    env = req.env,
                    platform = req.platform.mapNotNull { p ->
                        runCatching { Platform.valueOf(p.uppercase()) }.getOrNull()
                    }.toSet(),
                )
            }
            val timeout = config.timeout?.let(::parseDuration)
            val condition = config.condition?.takeIf { it.isNotBlank() }?.trim()
            val failureStrategy = resolveFailureStrategy(config.onFailure, config.retryAttempts)

            registry.add(
                SimpleTask(
                    id = taskId,
                    description = config.description.ifBlank { "Inline task: $taskId" },
                    phase = phase,
                    customDependencies = config.depends,
                    permissions = permissions,
                    requirements = requirements,
                    runtimeCondition = if (condition != null) {
                        { environment, _ -> evaluateCondition(condition, environment) }
                    } else {
                        null
                    },
                    failureStrategy = failureStrategy,
                    taskTimeout = timeout,
                    task = { environment, projectContext ->
                        try {
                            val executor = environment.service(CommandExecutor::class.java)
                            executor.execute(command, projectContext.dir.toString())
                            TaskResult.success("Task '$taskId' completed")
                        } catch (e: Exception) {
                            TaskResult.failure("Task '$taskId' failed: ${e.message}")
                        }
                    }
                )
            )
        }
    }

    override fun configSchema(): Map<String, Any> = mapOf(
        "type" to "object",
        "description" to "Inline task definitions. Keys are task IDs.",
        "x-permissions" to listOf(
            TaskPermission.FILE_SYSTEM_READ.wireName,
            TaskPermission.FILE_SYSTEM_WRITE.wireName,
            TaskPermission.NETWORK_OUTBOUND.wireName,
            TaskPermission.PROCESS_EXEC.wireName,
        ),
        "additionalProperties" to mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "description" to mapOf(
                    "type" to "string",
                    "description" to "Human-readable task description shown in task listings.",
                ),
                "run" to mapOf(
                    "type" to "string",
                    "description" to "Shell command to execute for this inline task.",
                ),
                "phase" to mapOf(
                    "type" to "string",
                    "description" to "Lifecycle phase for this task.",
                ),
                "depends" to mapOf(
                    "type" to "array",
                    "description" to "Task IDs that must complete before this task runs.",
                    "items" to mapOf("type" to "string"),
                ),
                "permissions" to mapOf(
                    "type" to "array",
                    "description" to "Explicit permissions required by the task.",
                    "items" to mapOf(
                        "type" to "string",
                        "enum" to listOf(
                            TaskPermission.FILE_SYSTEM_READ.wireName,
                            TaskPermission.FILE_SYSTEM_WRITE.wireName,
                            TaskPermission.NETWORK_OUTBOUND.wireName,
                            TaskPermission.PROCESS_EXEC.wireName,
                        ),
                    ),
                ),
                "requires" to mapOf(
                    "type" to "object",
                    "description" to "Runtime requirements checked before the task executes.",
                    "additionalProperties" to false,
                    "properties" to mapOf(
                        "tools" to mapOf(
                            "type" to "array",
                            "items" to mapOf("type" to "string"),
                        ),
                        "min-tool-versions" to mapOf(
                            "type" to "object",
                            "additionalProperties" to mapOf("type" to "string"),
                        ),
                        "env" to mapOf(
                            "type" to "array",
                            "items" to mapOf("type" to "string"),
                        ),
                        "platform" to mapOf(
                            "type" to "array",
                            "items" to mapOf("type" to "string"),
                        ),
                    ),
                ),
                "timeout" to mapOf(
                    "type" to "string",
                    "description" to "Maximum task duration such as 300s, 5m, or 1h.",
                ),
                "condition" to mapOf(
                    "type" to "string",
                    "description" to "Runtime condition expression, for example env.BRANCH == 'main'.",
                ),
                "onFailure" to mapOf(
                    "type" to "string",
                    "description" to "Failure strategy for the task.",
                    "enum" to listOf("ABORT", "CONTINUE", "RETRY"),
                    "default" to "ABORT",
                ),
                "retryAttempts" to mapOf(
                    "type" to "integer",
                    "description" to "Number of retry attempts when onFailure is RETRY.",
                    "minimum" to 1,
                ),
            ),
        ),
    )

    private fun resolvePhase(name: String): Phase? {
        val upper = name.uppercase()
        return runCatching { CoreWorkflow.valueOf(upper) }.getOrNull()
            ?: runCatching { CodeWorkflow.valueOf(upper) }.getOrNull()
            ?: runCatching { HooksWorkflow.valueOf(upper) }.getOrNull()
    }

    private fun resolveFailureStrategy(
        onFailure: String?,
        retryAttempts: Int?,
    ): FailureStrategy = when (onFailure?.trim()?.uppercase()) {
        null, "", "ABORT" -> FailureStrategy.ABORT
        "CONTINUE" -> FailureStrategy.CONTINUE
        "RETRY" -> FailureStrategy.RETRY(maxAttempts = retryAttempts?.coerceAtLeast(1) ?: 1)
        else -> FailureStrategy.ABORT
    }

    private fun parseDuration(value: String): Duration? {
        val match = DURATION_REGEX.matchEntire(value.trim()) ?: return null
        val amount = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        return when (unit) {
            "ms" -> Duration.ofMillis(amount)
            "m" -> Duration.ofMinutes(amount)
            "h" -> Duration.ofHours(amount)
            "", "s" -> Duration.ofSeconds(amount)
            else -> null
        }
    }

    private fun evaluateCondition(
        condition: String,
        environment: Environment,
    ): Boolean {
        val trimmed = condition.trim()

        if (trimmed.contains("==")) {
            val parts = trimmed.split("==", limit = 2).map { it.trim() }
            val variableName = resolveVariableName(parts.firstOrNull() ?: return false) ?: return false
            val expectedValue = unquote(parts.getOrNull(1) ?: return false)
            return environment.variable(variableName) == expectedValue
        }

        if (trimmed.contains("!=")) {
            val parts = trimmed.split("!=", limit = 2).map { it.trim() }
            val variableName = resolveVariableName(parts.firstOrNull() ?: return false) ?: return false
            val expectedValue = unquote(parts.getOrNull(1) ?: return false)
            return environment.variable(variableName) != expectedValue
        }

        val variableName = resolveVariableName(trimmed) ?: return false
        return environment.variable(variableName) != null
    }

    private fun resolveVariableName(expression: String): String? {
        val candidate = expression.trim().removePrefix("env.")
        return if (VARIABLE_NAME_REGEX.matches(candidate)) candidate else null
    }

    private fun unquote(value: String): String {
        val trimmed = value.trim()
        return if (
            trimmed.length >= 2 &&
            ((trimmed.startsWith("'") && trimmed.endsWith("'")) ||
                (trimmed.startsWith("\"") && trimmed.endsWith("\"")))
        ) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
    }

    private companion object {
        private val DURATION_REGEX = Regex("""^(\d+)(ms|s|m|h)?$""", RegexOption.IGNORE_CASE)
        private val VARIABLE_NAME_REGEX = Regex("""[A-Za-z_][A-Za-z0-9_]*""")
    }
}
