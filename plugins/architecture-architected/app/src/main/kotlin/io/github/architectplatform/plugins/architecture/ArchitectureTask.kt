package io.github.architectplatform.plugins.architecture

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase

/**
 * Task for validating architectural rules.
 *
 * Analyzes the project against defined architectural rules and generates a report.
 *
 * @property phase The workflow phase this task belongs to
 * @property context The architecture context containing rule configuration
 */
class ArchitectureTask(
    private val phase: Phase,
    private val context: ArchitectureContext,
) : Task {
    override val id: String = "architecture-validate"

    override fun phase(): Phase = phase

    override fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
        if (!context.enabled) {
            return TaskResult.success("Architecture plugin is disabled. Skipping validation.")
        }

        val allRules = context.rulesets.values.flatMap { it.rules } + context.customRules
        if (allRules.isEmpty()) {
            return TaskResult.success(
                "No architectural rules configured. Run 'architecture-init' to create a configuration.",
                listOf(
                    TaskResult.success("Hint: Add rules in your architect.yml under 'architecture' section"),
                    TaskResult.success("Or create a .architect/architecture.yml file with your rules")
                )
            )
        }

        return try {
            val rules = ArchitectureRules(context)
            val result = rules.validate(projectContext.dir)
            val report = when (context.reportFormat.lowercase()) {
                "json" -> rules.formatJsonReport(result)
                else -> rules.formatTextReport(result)
            }
            val shouldFail = result.shouldFail(context.strict, context.onViolation)

            if (shouldFail) {
                TaskResult.failure(
                    "Architecture validation failed with ${result.violations.size} violation(s)\n$report",
                    listOf(
                        TaskResult.failure("${result.violations.count { it.rule.severity == "error" }} error(s)"),
                        TaskResult.failure("${result.violations.count { it.rule.severity == "warning" }} warning(s)")
                    )
                )
            } else if (result.violations.isEmpty()) {
                TaskResult.success(
                    "Architecture validation passed successfully",
                    listOf(
                        TaskResult.success("All ${result.totalRulesChecked} rules satisfied"),
                        TaskResult.success("Analyzed ${result.filesAnalyzed} files")
                    )
                )
            } else {
                TaskResult.success(
                    "Architecture validation completed with warnings",
                    listOf(
                        TaskResult.success("${result.violations.size} warning(s) found"),
                        TaskResult.success("Use strict mode or onViolation: fail to treat warnings as errors")
                    )
                )
            }
        } catch (e: Exception) {
            TaskResult.failure(
                "Architecture validation failed with exception: ${e.message ?: "Unknown error"}",
                listOf(
                    TaskResult.failure("Exception details: ${e.javaClass.simpleName}"),
                    TaskResult.failure("Stack trace: ${e.stackTraceToString().take(500)}")
                )
            )
        }
    }
}
