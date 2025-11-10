package io.github.architectplatform.agent.dockercompose.domain

import java.time.Instant

/**
 * Domain model representing a deployment command for Docker Compose.
 * Follows clean domain model design - no framework dependencies.
 */
data class DeploymentCommand(
    val id: String,
    val resourceName: String,
    val projectName: String,
    val templates: List<String>,
    val variables: Map<String, Any>,
    val receivedAt: Instant = Instant.now()
) {
    fun withRenderedComposeFile(composeContent: String): DeploymentResult {
        return DeploymentResult(
            commandId = id,
            resourceName = resourceName,
            projectName = projectName,
            composeContent = composeContent,
            status = DeploymentStatus.PENDING
        )
    }
}

/**
 * Result of a Docker Compose deployment.
 */
data class DeploymentResult(
    val commandId: String,
    val resourceName: String,
    val projectName: String,
    val composeContent: String,
    val status: DeploymentStatus,
    val message: String? = null,
    val deployedServices: List<String> = emptyList(),
    val completedAt: Instant? = null
) {
    fun complete(services: List<String>, message: String? = null): DeploymentResult {
        return copy(
            status = DeploymentStatus.DEPLOYED,
            deployedServices = services,
            message = message,
            completedAt = Instant.now()
        )
    }

    fun fail(errorMessage: String): DeploymentResult {
        return copy(
            status = DeploymentStatus.FAILED,
            message = errorMessage,
            completedAt = Instant.now()
        )
    }
}

enum class DeploymentStatus {
    PENDING,
    DEPLOYING,
    DEPLOYED,
    FAILED
}
