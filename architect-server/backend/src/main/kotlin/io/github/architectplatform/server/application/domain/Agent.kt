package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model representing a registered agent.
 * Pure domain object without infrastructure concerns.
 */
data class Agent(
    val id: String,
    val agentType: AgentType,
    val namespace: String? = null,
    val capabilities: List<String> = emptyList(),
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
}

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
