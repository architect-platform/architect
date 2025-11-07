package io.github.architectplatform.plugins.architecture

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import java.io.File

/**
 * Architect plugin for architectural rule management and validation.
 *
 * This plugin enables projects to define and enforce architectural rules,
 * ensuring code consistency with the adopted architectural patterns and templates.
 *
 * Features:
 * - Define custom rulesets for architectural validation
 * - Support for dependency rules (forbidden/required dependencies)
 * - Support for naming conventions
 * - Support for structural rules
 * - Extensible with custom validators
 * - Multiple report formats (text, JSON, HTML)
 * - Configurable violation handling (warn/fail)
 *
 * Example configuration:
 * ```yaml
 * architecture:
 *   enabled: true
 *   rulesets:
 *     layered:
 *       enabled: true
 *       description: "Layered architecture rules"
 *       rules:
 *         - id: "no-direct-db-access"
 *           description: "Controllers should not directly access database"
 *           type: "dependency"
 *           pattern: ".*Controller.*"
 *           forbidden: [".*Repository.*", ".*DAO.*"]
 *         - id: "service-layer-required"
 *           description: "Controllers must use service layer"
 *           type: "dependency"
 *           pattern: ".*Controller.*"
 *           required: [".*Service.*"]
 *     naming:
 *       enabled: true
 *       description: "Naming convention rules"
 *       rules:
 *         - id: "controller-naming"
 *           description: "Controllers must end with Controller"
 *           type: "naming"
 *           pattern: ".*Controller"
 *           paths: ["src/main/.*Controller\\..*"]
 *   onViolation: "warn"  # or "fail"
 *   reportFormat: "text"  # or "json", "html"
 *   strict: false
 * ```
 *
 * Tasks:
 * - architecture-validate: Validates the project against defined architectural rules
 */
class ArchitecturePlugin : ArchitectPlugin<ArchitectureContext> {
    override val id = "architecture-plugin"
    override val contextKey: String = "architecture"
    override val ctxClass: Class<ArchitectureContext> = ArchitectureContext::class.java
    override var context: ArchitectureContext = ArchitectureContext()

    /**
     * Registers architecture tasks with the task registry.
     *
     * Registered tasks:
     * - architecture-validate: Validate architectural rules (VERIFY phase)
     *
     * @param registry The task registry to add tasks to
     */
    override fun register(registry: TaskRegistry) {
        registry.add(ArchitectureValidateTask(CodeWorkflow.VERIFY, context))
    }

    /**
     * Task for validating architectural rules.
     *
     * Analyzes the project against defined architectural rules and generates a report.
     */
    class ArchitectureValidateTask(
        private val phase: Phase,
        private val context: ArchitectureContext
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

            // Check if there are any rules configured
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

            try {
                // Create rules validator
                val rules = ArchitectureRules(context)
                
                // Validate the project
                val result = rules.validate(projectContext.dir)
                
                // Generate report
                val report = when (context.reportFormat.lowercase()) {
                    "json" -> rules.formatJsonReport(result)
                    else -> rules.formatTextReport(result)
                }
                
                // Print report
                println(report)
                
                // Determine if we should fail the build
                val shouldFail = result.shouldFail(context.strict, context.onViolation)
                
                return if (shouldFail) {
                    TaskResult.failure(
                        "Architecture validation failed with ${result.violations.size} violation(s)",
                        listOf(
                            TaskResult.failure("${result.violations.count { it.rule.severity == "error" }} error(s)"),
                            TaskResult.failure("${result.violations.count { it.rule.severity == "warning" }} warning(s)")
                        )
                    )
                } else {
                    if (result.violations.isEmpty()) {
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
                }
                
            } catch (e: Exception) {
                return TaskResult.failure(
                    "Architecture validation failed with exception: ${e.message ?: "Unknown error"}",
                    listOf(
                        TaskResult.failure("Exception details: ${e.javaClass.simpleName}"),
                        TaskResult.failure("Stack trace: ${e.stackTraceToString().take(500)}")
                    )
                )
            }
        }
    }
}
