package io.github.architectplatform.server.adapters.inbound.rest.dto

import io.github.architectplatform.server.application.domain.Agent
import io.github.architectplatform.server.application.domain.AgentStatus
import io.github.architectplatform.server.application.domain.AgentType
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class RegisterAgentRequest(
    val agentId: String,
    val agentType: AgentType,
    val namespace: String? = null,
    val capabilities: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serdeable
data class AgentHeartbeatRequest(
    val agentId: String,
    val status: AgentStatus = AgentStatus.HEALTHY,
    val metrics: Map<String, Any> = emptyMap()
)

@Serdeable
data class AgentResponse(
    val id: String,
    val agentType: String,
    val namespace: String?,
    val capabilities: List<String>,
    val metadata: Map<String, String>,
    val status: String,
    val registeredAt: String,
    val lastHeartbeat: String,
    val isHealthy: Boolean
) {
    companion object {
        fun fromDomain(agent: Agent) = AgentResponse(
            id = agent.id,
            agentType = agent.agentType.name,
            namespace = agent.namespace,
            capabilities = agent.capabilities,
            metadata = agent.metadata,
            status = agent.status.name,
            registeredAt = agent.registeredAt.toString(),
            lastHeartbeat = agent.lastHeartbeat.toString(),
            isHealthy = agent.isHealthy()
        )
    }
}
