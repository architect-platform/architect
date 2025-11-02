package io.github.architectplatform.plugins.github.dto

/**
 * Configuration for a GitHub Actions pipeline.
 *
 * Defines the parameters for creating a workflow file from a template.
 *
 * @property name The name of the workflow file (without .yml extension)
 * @property type The type of pipeline, corresponding to a template name
 * @property path File path pattern to trigger the workflow (e.g., "**" for all files)
 * @property branch The branch that triggers the workflow
 */
data class PipelineContext(
    val name: String,
    val type: String,
    val path: String = "**",
    val branch: String = "main",
)
