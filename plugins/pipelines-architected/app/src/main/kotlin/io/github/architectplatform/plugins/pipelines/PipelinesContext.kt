package io.github.architectplatform.plugins.pipelines

/**
 * Configuration context for the Pipelines plugin.
 *
 * Contains workflow definitions and execution settings.
 *
 * @property workflows List of workflow definitions
 * @property enabled Whether the pipelines plugin is enabled
 */
data class PipelinesContext(
    val workflows: List<WorkflowDefinition> = emptyList(),
    val enabled: Boolean = true
)

/**
 * Represents a complete workflow definition.
 *
 * @property name Unique name of the workflow
 * @property description Optional description of the workflow
 * @property extends Optional name of template workflow to extend
 * @property steps List of workflow steps to execute
 * @property env Environment variables for the workflow
 */
data class WorkflowDefinition(
    val name: String,
    val description: String = "",
    val extends: String? = null,
    val steps: List<WorkflowStep> = emptyList(),
    val env: Map<String, String> = emptyMap()
)

/**
 * Represents a single step in a workflow.
 *
 * @property name Name of the step
 * @property task Architect task to execute
 * @property args Arguments to pass to the task
 * @property dependsOn List of step names this step depends on
 * @property continueOnError Whether to continue if this step fails
 * @property condition Optional condition expression to evaluate before running
 */
data class WorkflowStep(
    val name: String,
    val task: String,
    val args: List<String> = emptyList(),
    val dependsOn: List<String> = emptyList(),
    val continueOnError: Boolean = false,
    val condition: String? = null
)

/**
 * Result of workflow execution.
 *
 * @property workflowName Name of the workflow
 * @property success Whether the workflow executed successfully
 * @property stepResults Results from individual steps
 * @property message Overall result message
 */
data class WorkflowExecutionResult(
    val workflowName: String,
    val success: Boolean,
    val stepResults: List<StepExecutionResult>,
    val message: String
)

/**
 * Result of a single step execution.
 *
 * @property stepName Name of the step
 * @property success Whether the step succeeded
 * @property message Result message
 * @property skipped Whether the step was skipped
 */
data class StepExecutionResult(
    val stepName: String,
    val success: Boolean,
    val message: String,
    val skipped: Boolean = false
)
