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
     * Logs warnings when template loading fails.
     */
    private fun resolveWorkflow(workflow: WorkflowDefinition, projectContext: ProjectContext): WorkflowDefinition {
        val extendsTemplate = workflow.extends ?: return workflow

        // Try to load template from .architect/pipelines
        val templateFile = File(projectContext.dir.toFile(), ".architect/pipelines/$extendsTemplate.yml")
        if (templateFile.exists()) {
            try {
                val template = yamlMapper.readValue(templateFile, WorkflowDefinition::class.java)
                return mergeWorkflows(template, workflow)
            } catch (e: Exception) {
                System.err.println("Warning: Failed to parse template file '$extendsTemplate.yml': ${e.message}")
                return workflow
            }
        }

        // Try to load from embedded resources
        try {
            val content = this.javaClass.classLoader.getResourceAsStream(
                "workflows/templates/$extendsTemplate.yml"
            )?.bufferedReader()?.use { it.readText() }

            if (content == null) {
                System.err.println("Warning: Template '$extendsTemplate' not found in .architect/pipelines/ or embedded resources")
                return workflow
            }

            val template = yamlMapper.readValue(content, WorkflowDefinition::class.java)
            return mergeWorkflows(template, workflow)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to load template '$extendsTemplate': ${e.message}")
            return workflow
        }
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
     * Uses topological sorting to ensure dependencies are met.
     */
    private fun executeWorkflowSteps(
        workflow: WorkflowDefinition,
        environment: Environment,
        projectContext: ProjectContext
    ): WorkflowExecutionResult {
        // Sort steps by dependencies using topological sort
        val sortedSteps = try {
            topologicalSort(workflow.steps)
        } catch (e: IllegalArgumentException) {
            return WorkflowExecutionResult(
                workflowName = workflow.name,
                success = false,
                stepResults = listOf(
                    StepExecutionResult(
                        stepName = "workflow",
                        success = false,
                        message = "Dependency cycle detected: ${e.message}"
                    )
                ),
                message = "Failed to resolve step dependencies"
            )
        }

        val stepResults = mutableListOf<StepExecutionResult>()
        val executedSteps = mutableSetOf<String>()
        val failedSteps = mutableSetOf<String>()

        for (step in sortedSteps) {
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
     * Performs topological sort on workflow steps based on dependencies.
     * Throws IllegalArgumentException if a cycle is detected.
     */
    private fun topologicalSort(steps: List<WorkflowStep>): List<WorkflowStep> {
        val stepMap = steps.associateBy { it.name }
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val sorted = mutableListOf<WorkflowStep>()

        fun visit(stepName: String) {
            if (stepName in visited) return
            if (stepName in visiting) {
                throw IllegalArgumentException("Cycle detected at step '$stepName'")
            }

            val step = stepMap[stepName] ?: throw IllegalArgumentException("Unknown step dependency: $stepName")
            visiting.add(stepName)

            // Visit dependencies first
            for (dep in step.dependsOn) {
                visit(dep)
            }

            visiting.remove(stepName)
            visited.add(stepName)
            sorted.add(step)
        }

        // Visit all steps
        for (step in steps) {
            if (step.name !in visited) {
                visit(step.name)
            }
        }

        return sorted
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
     * Returns false if condition cannot be parsed to ensure safety.
     * 
     * Supported formats:
     * - "VAR == value": Check if variable equals value
     * - "VAR != value": Check if variable doesn't equal value
     * - "VAR": Check if variable exists
     */
    private fun evaluateCondition(condition: String, env: Map<String, String>): Boolean {
        val trimmed = condition.trim()
        
        // Check for equality comparison
        if (trimmed.contains("==")) {
            val parts = trimmed.split("==", limit = 2).map { it.trim() }
            if (parts.size != 2) {
                System.err.println("Warning: Invalid condition format '$condition', expected 'VAR == value'")
                return false
            }
            val envValue = env[parts[0]] ?: System.getenv(parts[0])
            return envValue == parts[1]
        }
        
        // Check for inequality comparison
        if (trimmed.contains("!=")) {
            val parts = trimmed.split("!=", limit = 2).map { it.trim() }
            if (parts.size != 2) {
                System.err.println("Warning: Invalid condition format '$condition', expected 'VAR != value'")
                return false
            }
            val envValue = env[parts[0]] ?: System.getenv(parts[0])
            return envValue != parts[1]
        }
        
        // Check if variable exists (no operator)
        if (trimmed.matches(Regex("[A-Z_][A-Z0-9_]*"))) {
            return env.containsKey(trimmed) || System.getenv(trimmed) != null
        }
        
        // Invalid condition format
        System.err.println("Warning: Invalid condition format '$condition'. Supported formats: 'VAR == value', 'VAR != value', or 'VAR' (existence check)")
        return false
    }
}
