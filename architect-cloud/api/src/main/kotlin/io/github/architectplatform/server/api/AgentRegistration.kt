package io.github.architectplatform.server.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Request to register an agent with the server
 */
data class RegisterAgentRequest(
    @JsonProperty("agentId")
    val agentId: String,
    
    @JsonProperty("agentType")
    val agentType: AgentType,
    
    @JsonProperty("namespace")
    val namespace: String? = null,
    
    @JsonProperty("capabilities")
    val capabilities: List<String> = emptyList(),
    
    @JsonProperty("metadata")
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Response after agent registration
 */
data class RegisterAgentResponse(
    @JsonProperty("agentId")
    val agentId: String,
    
    @JsonProperty("registeredAt")
    val registeredAt: Instant,
    
    @JsonProperty("serverVersion")
    val serverVersion: String? = null,
    
    @JsonProperty("token")
    val token: String? = null
)

/**
 * Heartbeat request from agent
 */
data class AgentHeartbeatRequest(
    @JsonProperty("agentId")
    val agentId: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @JsonProperty("status")
    val status: AgentStatus = AgentStatus.HEALTHY,
    
    @JsonProperty("metrics")
    val metrics: Map<String, Any> = emptyMap()
)

/**
 * Agent type enumeration
 */
enum class AgentType {
    KUBERNETES,
    DOCKER,
    CLOUD_RUN,
    ECS,
    GENERIC
}

/**
 * Agent status
 */
enum class AgentStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    OFFLINE
}
