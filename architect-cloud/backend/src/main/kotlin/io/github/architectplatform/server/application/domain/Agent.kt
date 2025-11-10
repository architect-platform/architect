package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model representing a registered agent.
 * Pure domain object without infrastructure concerns.
 * 
 * NEW: Enhanced with environment support and cloud provider targeting
 */
data class Agent(
    val id: String,
    val agentType: AgentType,
    val namespace: String? = null,
    val capabilities: List<String> = emptyList(),
    val supportedEnvironments: List<String> = listOf("development"),
    val cloudProvider: String? = null,
    val region: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val status: AgentStatus = AgentStatus.HEALTHY,
    val registeredAt: Instant = Instant.now(),
    val lastHeartbeat: Instant = Instant.now()
) {
    fun updateHeartbeat(status: AgentStatus = AgentStatus.HEALTHY): Agent {
        return copy(
            status = status,
            lastHeartbeat = Instant.now()
        )
    }

    fun markOffline(): Agent {
        return copy(status = AgentStatus.OFFLINE)
    }

    fun isHealthy(): Boolean {
        val fiveMinutesAgo = Instant.now().minusSeconds(300)
        return status == AgentStatus.HEALTHY && lastHeartbeat.isAfter(fiveMinutesAgo)
    }
    
    /**
     * Check if agent supports the given environment
     */
    fun supportsEnvironment(environment: String): Boolean {
        return supportedEnvironments.contains(environment)
    }
    
    /**
     * Check if agent can handle the given cloud provider
     */
    fun supportsCloudProvider(provider: String): Boolean {
        return cloudProvider == null || cloudProvider == provider
    }
}

/**
 * Agent type enumeration - supports multiple orchestration platforms
 */
enum class AgentType {
    KUBERNETES,
    DOCKER_COMPOSE,
    DOCKER_SWARM,
    AWS_ECS,
    AWS_FARGATE,
    GOOGLE_CLOUD_RUN,
    AZURE_CONTAINER_INSTANCES,
    NOMAD,
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
