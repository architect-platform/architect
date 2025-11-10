package io.github.architectplatform.server.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Deployment command sent from server to agent
 */
data class DeploymentCommandDTO(
    @JsonProperty("commandId")
    val commandId: String,
    
    @JsonProperty("resourceName")
    val resourceName: String,
    
    @JsonProperty("namespace")
    val namespace: String,
    
    @JsonProperty("templates")
    val templates: List<String>,
    
    @JsonProperty("variables")
    val variables: Map<String, Any>,
    
    @JsonProperty("operation")
    val operation: DeploymentOperation = DeploymentOperation.APPLY,
    
    @JsonProperty("createdAt")
    val createdAt: Instant = Instant.now()
)

/**
 * Deployment result sent from agent to server
 */
data class DeploymentResultDTO(
    @JsonProperty("commandId")
    val commandId: String,
    
    @JsonProperty("resourceName")
    val resourceName: String,
    
    @JsonProperty("namespace")
    val namespace: String,
    
    @JsonProperty("status")
    val status: DeploymentStatusDTO,
    
    @JsonProperty("message")
    val message: String? = null,
    
    @JsonProperty("appliedResources")
    val appliedResources: List<AppliedResourceDTO> = emptyList(),
    
    @JsonProperty("completedAt")
    val completedAt: Instant? = null,
    
    @JsonProperty("error")
    val error: String? = null
)

/**
 * Applied Kubernetes resource information
 */
data class AppliedResourceDTO(
    @JsonProperty("kind")
    val kind: String,
    
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("namespace")
    val namespace: String,
    
    @JsonProperty("apiVersion")
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
 * Deployment status
 */
enum class DeploymentStatusDTO {
    PENDING,
    DEPLOYING,
    DEPLOYED,
    FAILED,
    ROLLED_BACK
}
