package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model for tracking deployment history.
 * Records all deployments, rollbacks, and their outcomes.
 */
data class DeploymentHistory(
    val id: String,
    val applicationDefinitionId: String,
    val applicationName: String,
    val version: String,
    val deploymentCommandId: String,
    val operation: DeploymentOperation,
    val environment: String,
    val agentId: String,
    val status: CommandStatus,
    val success: Boolean,
    val message: String? = null,
    val deployedBy: String? = null,  // User or system that triggered deployment
    val deploymentUrl: String? = null,
    val healthStatus: String? = null,
    val tags: Map<String, String> = emptyMap(),
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val duration: Long? = null  // Duration in seconds
) {
    fun complete(success: Boolean, message: String? = null, deploymentUrl: String? = null): DeploymentHistory {
        val now = Instant.now()
        return copy(
            status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED,
            success = success,
            message = message,
            deploymentUrl = deploymentUrl,
            completedAt = now,
            duration = java.time.Duration.between(startedAt, now).seconds
        )
    }
    
    companion object {
        fun fromDeploymentCommand(command: DeploymentCommand): DeploymentHistory {
            return DeploymentHistory(
                id = command.id + "-history",
                applicationDefinitionId = command.applicationDefinitionId,
                applicationName = command.applicationName,
                version = command.deploymentVersion.toString(),
                deploymentCommandId = command.id,
                operation = command.operation,
                environment = command.targetEnvironment,
                agentId = command.agentId,
                status = command.status,
                success = command.result?.success ?: false,
                message = command.result?.message,
                deploymentUrl = command.result?.deploymentUrl,
                healthStatus = command.result?.healthStatus,
                tags = command.tags,
                startedAt = command.sentAt ?: command.createdAt,
                completedAt = command.completedAt
            )
        }
    }
}
