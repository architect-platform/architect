package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Platform-agnostic deployment command.
 * Clean domain model without platform-specific concepts.
 * 
 * NEW: Enhanced with rollback support, deployment history, and tracking
 */
data class DeploymentCommand(
    val id: String,
    val agentId: String,
    val applicationDefinitionId: String,
    val applicationName: String,
    val templates: List<String>,
    val variables: Map<String, Any>,
    val dependencies: List<String> = emptyList(),
    val operation: DeploymentOperation = DeploymentOperation.APPLY,
    val status: CommandStatus = CommandStatus.PENDING,
    val targetEnvironment: String = "development",
    val previousCommandId: String? = null,  // For rollback tracking
    val deploymentVersion: Int = 1,
    val tags: Map<String, String> = emptyMap(),
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
    
    fun markCancelled(): DeploymentCommand {
        return copy(
            status = CommandStatus.CANCELLED,
            completedAt = Instant.now()
        )
    }
    
    /**
     * Create a rollback command based on this deployment
     */
    fun createRollbackCommand(newId: String): DeploymentCommand {
        return copy(
            id = newId,
            operation = DeploymentOperation.ROLLBACK,
            previousCommandId = this.id,
            status = CommandStatus.PENDING,
            sentAt = null,
            completedAt = null,
            result = null,
            createdAt = Instant.now()
        )
    }
    
    /**
     * Check if this is a rollback operation
     */
    fun isRollback(): Boolean = operation == DeploymentOperation.ROLLBACK
    
    /**
     * Check if deployment is in final state
     */
    fun isFinal(): Boolean = status in listOf(CommandStatus.COMPLETED, CommandStatus.FAILED, CommandStatus.CANCELLED)
}

/**
 * Result of a deployment operation
 */
data class DeploymentResult(
    val success: Boolean,
    val message: String? = null,
    val appliedResources: List<AppliedResource> = emptyList(),
    val error: String? = null,
    val deploymentUrl: String? = null,  // URL to access deployed application
    val healthStatus: String? = null     // Health check status
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
