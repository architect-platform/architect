package io.github.architectplatform.agent.domain

import java.time.Instant

/**
 * Domain model representing a deployment command received from Architect Server.
 * Pure domain object without infrastructure concerns.
 */
data class DeploymentCommand(
    val id: String,
    val resourceName: String,
    val namespace: String,
    val templates: List<String>,
    val variables: Map<String, Any>,
    val receivedAt: Instant = Instant.now()
) {
    fun withRenderedManifests(manifests: List<String>): DeploymentResult {
        return DeploymentResult(
            commandId = id,
            resourceName = resourceName,
            namespace = namespace,
            manifests = manifests,
            status = DeploymentStatus.PENDING
        )
    }
}

/**
 * Result of a deployment operation
 */
data class DeploymentResult(
    val commandId: String,
    val resourceName: String,
    val namespace: String,
    val manifests: List<String>,
    val status: DeploymentStatus,
    val message: String? = null,
    val appliedResources: List<AppliedResource> = emptyList(),
    val completedAt: Instant? = null
) {
    fun complete(resources: List<AppliedResource>, message: String? = null): DeploymentResult {
        return copy(
            status = DeploymentStatus.DEPLOYED,
            appliedResources = resources,
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

/**
 * Represents a Kubernetes resource that was applied
 */
data class AppliedResource(
    val kind: String,
    val name: String,
    val namespace: String,
    val apiVersion: String
)

enum class DeploymentStatus {
    PENDING,
    DEPLOYING,
    DEPLOYED,
    FAILED
}
