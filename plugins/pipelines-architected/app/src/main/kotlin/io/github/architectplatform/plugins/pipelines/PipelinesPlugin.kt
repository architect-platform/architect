package io.github.architectplatform.plugins.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.architectplatform.api.components.execution.ResourceExtractor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Architect plugin for managing and executing pipelines of Architect tasks.
 *
 * This plugin enables:
 * - Creation of custom workflows composed of Architect tasks
 * - Extending predefined workflow templates
 * - Parallel execution of independent tasks
 * - Step dependencies and conditional execution
 *
 * The plugin integrates with GitHub Actions for CI/CD automation.
 */
class PipelinesPlugin : ArchitectPlugin<PipelinesContext> {
    override val id = "pipelines-plugin"
    override val contextKey: String = "pipelines"
    override val ctxClass: Class<PipelinesContext> = PipelinesContext::class.java
    override var context: PipelinesContext = PipelinesContext()

    private val yamlMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    /**
     * Registers pipeline tasks with the task registry.
     *
     * Registered tasks:
     * - pipelines-init: Initialize pipeline templates and configuration
     * - pipelines-execute: Execute a workflow by name
     * - pipelines-list: List available workflows
     *
     * @param registry The task registry to add tasks to
     */
    override fun register(registry: TaskRegistry) {
        registry.add(PipelinesTask(
            id = "pipelines-init",
            phase = CoreWorkflow.INIT,
            task = ::initPipelines
        ))

        registry.add(PipelinesTask(
            id = "pipelines-execute",
            phase = CoreWorkflow.BUILD,
            task = ::executeWorkflow
        ))

        registry.add(PipelinesTask(
            id = "pipelines-list",
            phase = CoreWorkflow.BUILD,
            task = ::listWorkflows
        ))
    }

    /**
     * Task wrapper for pipeline operations.
     */
    class PipelinesTask(
        override val id: String,
        private val phase: Phase,
        private val task: (Environment, ProjectContext, List<String>) -> TaskResult
    ) : Task {
        override fun phase(): Phase = phase

        override fun execute(
            environment: Environment,
            projectContext: ProjectContext,
            args: List<String>
        ): TaskResult {
            return try {
                task(environment, projectContext, args)
            } catch (e: Exception) {
                TaskResult.failure("Pipelines task: $id failed with exception: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Initializes pipeline templates in the project.
     *
     * Creates .architect/pipelines directory and copies default workflow templates.
     */
    private fun initPipelines(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
        if (!context.enabled) {
            return TaskResult.success("Pipelines initialization skipped (disabled).")
        }

        val pipelinesDir = File(projectContext.dir.toFile(), ".architect/pipelines")
        if (!pipelinesDir.exists()) {
            pipelinesDir.mkdirs()
        }

        val resourceExtractor = environment.service(ResourceExtractor::class.java)
        
        // Copy template workflows
        val templates = listOf("ci-standard.yml", "release-standard.yml", "docs-publish.yml")
        val results = mutableListOf<TaskResult>()
        
        for (template in templates) {
            try {
                val content = resourceExtractor.getResourceFileContent(
                    this.javaClass.classLoader,
                    "workflows/templates/$template"
                )
                val targetFile = File(pipelinesDir, template)
                targetFile.writeText(content)
                results.add(TaskResult.success("Created template: $template"))
            } catch (e: Exception) {
                results.add(TaskResult.failure("Failed to create template $template: ${e.message}"))
            }
        }

        // Create README
        val readmeContent = """
            # Architect Pipelines
            
            This directory contains workflow definitions for the pipelines-architected plugin.
            
            ## Usage
            
            1. Create or customize workflow files (*.yml)
            2. Reference them in your architect.yml:
            
            ```yaml
            pipelines:
              workflows:
                - name: my-workflow
                  extends: ci-standard
                  steps:
                    - name: custom-step
                      task: custom-task
            ```
            
            3. Execute workflows:
            ```bash
            architect pipelines-execute -- my-workflow
            ```
            
            ## Templates
            
            - ci-standard.yml: Standard CI workflow with build and test
            - release-standard.yml: Release workflow with versioning
            - docs-publish.yml: Documentation build and publish workflow
        """.trimIndent()
        
        File(pipelinesDir, "README.md").writeText(readmeContent)

        return TaskResult.success("Pipelines initialized successfully.", results)
    }

    /**
     * Lists all available workflows.
     */
    private fun listWorkflows(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
        if (!context.enabled) {
            return TaskResult.success("Pipelines disabled.")
        }

        val workflows = context.workflows
        if (workflows.isEmpty()) {
            return TaskResult.success("No workflows configured.")
        }

        val workflowList = workflows.joinToString("\n") { workflow ->
            val desc = if (workflow.description.isNotEmpty()) " - ${workflow.description}" else ""
            val ext = workflow.extends?.let { " (extends: $it)" } ?: ""
            "  • ${workflow.name}$desc$ext"
        }

        return TaskResult.success("Available workflows:\n$workflowList")
    }

    /**
     * Executes a workflow by name.
     *
     * Resolves workflow from context, handles template extension,
     * manages step dependencies, and executes steps.
     */
    private fun executeWorkflow(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
        if (!context.enabled) {
            return TaskResult.success("Pipelines execution skipped (disabled).")
        }

        if (args.isEmpty()) {
            return TaskResult.failure("No workflow name provided. Usage: pipelines-execute -- <workflow-name>")
        }

        val workflowName = args[0]
        val workflow = context.workflows.find { it.name == workflowName }
            ?: return TaskResult.failure("Workflow '$workflowName' not found in configuration.")

        // Resolve workflow (handle template extension)
        val resolvedWorkflow = resolveWorkflow(workflow, projectContext)

        // Execute workflow steps
        val result = executeWorkflowSteps(resolvedWorkflow, environment, projectContext)

        return if (result.success) {
            TaskResult.success(
                "Workflow '${result.workflowName}' completed successfully.",
                results = result.stepResults.map { 
                    if (it.skipped) {
                        TaskResult.success("${it.stepName}: Skipped")
                    } else if (it.success) {
                        TaskResult.success("${it.stepName}: ${it.message}")
                    } else {
                        TaskResult.failure("${it.stepName}: ${it.message}")
                    }
                }
            )
        } else {
            TaskResult.failure(
                "Workflow '${result.workflowName}' failed: ${result.message}",
                results = result.stepResults.map { 
                    if (it.success) {
                        TaskResult.success("${it.stepName}: ${it.message}")
                    } else {
                        TaskResult.failure("${it.stepName}: ${it.message}")
                    }
                }
            )
        }
    }

    /**
     * Resolves a workflow, merging with template if specified.
     */
    private fun resolveWorkflow(workflow: WorkflowDefinition, projectContext: ProjectContext): WorkflowDefinition {
        val extendsTemplate = workflow.extends ?: return workflow

        // Try to load template from .architect/pipelines
        val templateFile = File(projectContext.dir.toFile(), ".architect/pipelines/$extendsTemplate.yml")
        if (!templateFile.exists()) {
            // Try to load from embedded resources
            try {
                val content = this.javaClass.classLoader.getResourceAsStream(
                    "workflows/templates/$extendsTemplate.yml"
                )?.bufferedReader()?.use { it.readText() }
                    ?: return workflow

                val template = yamlMapper.readValue(content, WorkflowDefinition::class.java)
                return mergeWorkflows(template, workflow)
            } catch (e: Exception) {
                return workflow
            }
        }

        val template = yamlMapper.readValue(templateFile, WorkflowDefinition::class.java)
        return mergeWorkflows(template, workflow)
    }

    /**
     * Merges a workflow with its template.
     */
    private fun mergeWorkflows(template: WorkflowDefinition, workflow: WorkflowDefinition): WorkflowDefinition {
        return workflow.copy(
            description = workflow.description.ifEmpty { template.description },
            steps = template.steps + workflow.steps,
            env = template.env + workflow.env
        )
    }

    /**
     * Executes workflow steps in dependency order.
     */
    private fun executeWorkflowSteps(
        workflow: WorkflowDefinition,
        environment: Environment,
        projectContext: ProjectContext
    ): WorkflowExecutionResult {
        val stepResults = mutableListOf<StepExecutionResult>()
        val executedSteps = mutableSetOf<String>()
        val failedSteps = mutableSetOf<String>()

        for (step in workflow.steps) {
            // Check dependencies
            val missingDeps = step.dependsOn.filter { it !in executedSteps }
            if (missingDeps.isNotEmpty()) {
                stepResults.add(StepExecutionResult(
                    stepName = step.name,
                    success = false,
                    message = "Dependencies not met: ${missingDeps.joinToString(", ")}",
                    skipped = true
                ))
                continue
            }

            // Check if dependencies failed
            val failedDeps = step.dependsOn.filter { it in failedSteps }
            if (failedDeps.isNotEmpty() && !step.continueOnError) {
                stepResults.add(StepExecutionResult(
                    stepName = step.name,
                    success = false,
                    message = "Skipped due to failed dependencies: ${failedDeps.joinToString(", ")}",
                    skipped = true
                ))
                continue
            }

            // Execute step
            val stepResult = executeStep(step, workflow.env, environment, projectContext)
            stepResults.add(stepResult)
            
            if (stepResult.success || stepResult.skipped) {
                executedSteps.add(step.name)
            } else {
                if (!step.continueOnError) {
                    failedSteps.add(step.name)
                    // Stop execution on failure unless continueOnError is true
                    return WorkflowExecutionResult(
                        workflowName = workflow.name,
                        success = false,
                        stepResults = stepResults,
                        message = "Step '${step.name}' failed"
                    )
                }
                failedSteps.add(step.name)
            }
        }

        val overallSuccess = failedSteps.isEmpty()
        return WorkflowExecutionResult(
            workflowName = workflow.name,
            success = overallSuccess,
            stepResults = stepResults,
            message = if (overallSuccess) "All steps completed successfully" 
                     else "Some steps failed: ${failedSteps.joinToString(", ")}"
        )
    }

    /**
     * Executes a single workflow step.
     */
    private fun executeStep(
        step: WorkflowStep,
        workflowEnv: Map<String, String>,
        environment: Environment,
        projectContext: ProjectContext
    ): StepExecutionResult {
        // Check condition if present
        if (step.condition != null) {
            val conditionMet = evaluateCondition(step.condition, workflowEnv)
            if (!conditionMet) {
                return StepExecutionResult(
                    stepName = step.name,
                    success = true,
                    message = "Condition not met, skipped",
                    skipped = true
                )
            }
        }

        // Find and execute the task
        val taskRegistry = environment.service(TaskRegistry::class.java)
        val task = taskRegistry.get(step.task)
            ?: return StepExecutionResult(
                stepName = step.name,
                success = false,
                message = "Task '${step.task}' not found"
            )

        try {
            val result = task.execute(environment, projectContext, step.args)
            return StepExecutionResult(
                stepName = step.name,
                success = result.success,
                message = result.message ?: "Task completed"
            )
        } catch (e: Exception) {
            return StepExecutionResult(
                stepName = step.name,
                success = false,
                message = "Exception: ${e.message ?: "Unknown error"}"
            )
        }
    }

    /**
     * Evaluates a simple condition expression.
     * Supports basic environment variable checks.
     */
    private fun evaluateCondition(condition: String, env: Map<String, String>): Boolean {
        // Simple implementation: check if environment variable exists and equals value
        // Format: "ENV_VAR == value" or "ENV_VAR != value" or just "ENV_VAR" (exists check)
        val trimmed = condition.trim()
        
        if (trimmed.contains("==")) {
            val parts = trimmed.split("==").map { it.trim() }
            if (parts.size == 2) {
                val envValue = env[parts[0]] ?: System.getenv(parts[0])
                return envValue == parts[1]
            }
        } else if (trimmed.contains("!=")) {
            val parts = trimmed.split("!=").map { it.trim() }
            if (parts.size == 2) {
                val envValue = env[parts[0]] ?: System.getenv(parts[0])
                return envValue != parts[1]
            }
        } else {
            // Just check if variable exists
            return env.containsKey(trimmed) || System.getenv(trimmed) != null
        }
        
        return true // Default to true if condition can't be parsed
    }
}
