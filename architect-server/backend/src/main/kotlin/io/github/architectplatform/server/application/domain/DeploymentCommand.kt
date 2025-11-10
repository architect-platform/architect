package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model representing a deployment command to be sent to an agent.
 * Pure domain object without infrastructure concerns.
 */
data class DeploymentCommand(
    val id: String,
    val agentId: String,
    val resourceDefinitionId: String,
    val resourceName: String,
    val namespace: String,
    val templates: List<String>,
    val variables: Map<String, Any>,
    val operation: DeploymentOperation = DeploymentOperation.APPLY,
    val status: CommandStatus = CommandStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val completedAt: Instant? = null,
    val result: DeploymentResult? = null
) {
    fun markSent(): DeploymentCommand {
        return copy(
            status = CommandStatus.SENT,
            sentAt = Instant.now()
        )
    }

    fun markCompleted(result: DeploymentResult): DeploymentCommand {
        return copy(
            status = CommandStatus.COMPLETED,
            result = result,
            completedAt = Instant.now()
        )
    }

    fun markFailed(errorMessage: String): DeploymentCommand {
        return copy(
            status = CommandStatus.FAILED,
            result = DeploymentResult(
                success = false,
                message = errorMessage,
                appliedResources = emptyList()
            ),
            completedAt = Instant.now()
        )
    }
}

/**
 * Result of a deployment operation
 */
data class DeploymentResult(
    val success: Boolean,
    val message: String? = null,
    val appliedResources: List<AppliedResource> = emptyList(),
    val error: String? = null
)

/**
 * Represents a Kubernetes resource that was applied
 */
data class AppliedResource(
    val kind: String,
    val name: String,
    val namespace: String,
    val apiVersion: String
)

/**
 * Deployment operation type
 */
enum class DeploymentOperation {
    APPLY,
    DELETE,
    UPDATE,
    ROLLBACK
}

/**
 * Command status
 */
enum class CommandStatus {
    PENDING,
    SENT,
    COMPLETED,
    FAILED,
    CANCELLED
}
