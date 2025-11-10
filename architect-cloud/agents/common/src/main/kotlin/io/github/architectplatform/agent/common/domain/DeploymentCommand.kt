package io.github.architectplatform.agent.common.domain

import java.time.Instant

/**
 * Common deployment command domain model.
 * Shared across all agent types.
 */
data class DeploymentCommand(
    val id: String,
    val agentId: String,
    val applicationDefinitionId: String,
    val applicationName: String,
    val resourceName: String,
    val templates: List<String>,
    val variables: Map<String, Any>,
    val operation: DeploymentOperation = DeploymentOperation.APPLY,
    val targetEnvironment: String = "development",
    val tags: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now()
)

/**
 * Deployment operation types
 */
enum class DeploymentOperation {
    APPLY,
    DELETE,
    UPDATE,
    ROLLBACK
}

/**
 * Result of a deployment operation
 */
data class DeploymentResult(
    val commandId: String,
    val success: Boolean,
    val message: String? = null,
    val appliedResources: List<AppliedResource> = emptyList(),
    val error: String? = null,
    val deploymentUrl: String? = null,
    val healthStatus: String? = null
)

/**
 * Applied resource information
 */
data class AppliedResource(
    val kind: String,
    val name: String,
    val namespace: String? = null,
    val status: String? = null
)

/**
 * Deployment status
 */
enum class DeploymentStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    ROLLED_BACK
}
