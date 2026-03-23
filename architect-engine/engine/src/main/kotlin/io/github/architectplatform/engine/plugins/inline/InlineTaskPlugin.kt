package io.github.architectplatform.engine.plugins.inline

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.components.workflows.hooks.HooksWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.impl.SimpleTask
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.engine.plugins.inline.context.InlineTaskConfig

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

            registry.add(
                SimpleTask(
                    id = taskId,
                    description = config.description.ifBlank { "Inline task: $taskId" },
                    phase = phase,
                    customDependencies = config.depends,
                    permissions = permissions,
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

    private fun resolvePhase(name: String): Phase? {
        val upper = name.uppercase()
        return runCatching { CoreWorkflow.valueOf(upper) }.getOrNull()
            ?: runCatching { CodeWorkflow.valueOf(upper) }.getOrNull()
            ?: runCatching { HooksWorkflow.valueOf(upper) }.getOrNull()
    }
}
