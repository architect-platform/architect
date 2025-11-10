package io.github.architectplatform.server.application.services

import io.github.architectplatform.server.application.domain.Agent
import io.github.architectplatform.server.application.domain.AgentStatus
import io.github.architectplatform.server.application.domain.AgentType
import io.github.architectplatform.server.application.ports.inbound.ManageAgentUseCase
import io.github.architectplatform.server.application.ports.outbound.AgentPort
import io.github.architectplatform.server.application.services.EventBroadcastService
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service implementing agent management use cases.
 * Follows single responsibility principle - handles only agent operations.
 */
@Singleton
class AgentService(
    private val agentPort: AgentPort,
    private val eventBroadcastService: EventBroadcastService
) : ManageAgentUseCase {

    private val logger = LoggerFactory.getLogger(AgentService::class.java)

    override fun registerAgent(
        agentId: String,
        agentType: AgentType,
        namespace: String?,
        capabilities: List<String>,
        metadata: Map<String, String>
    ): Agent {
        logger.info("Registering agent: $agentId of type: $agentType")
        
        // Check if agent already exists
        val existing = agentPort.findById(agentId)
        if (existing != null) {
            logger.info("Agent already registered, updating: $agentId")
            return existing.updateHeartbeat()
                .let { agentPort.save(it) }
        }
        
        val agent = Agent(
            id = agentId,
            agentType = agentType,
            namespace = namespace,
            capabilities = capabilities,
            metadata = metadata
        )
        
        return agentPort.save(agent).also {
            eventBroadcastService.broadcastAgentRegistered(it)
            logger.info("Agent registered: $agentId")
        }
    }

    override fun recordHeartbeat(
        agentId: String,
        status: AgentStatus,
        metrics: Map<String, Any>
    ): Agent? {
        logger.debug("Recording heartbeat for agent: $agentId")
        
        val agent = agentPort.findById(agentId) ?: run {
            logger.warn("Agent not found for heartbeat: $agentId")
            return null
        }
        
        val updated = agent.updateHeartbeat(status)
        return agentPort.save(updated).also {
            eventBroadcastService.broadcastAgentHeartbeat(it)
        }
    }

    override fun getAgent(agentId: String): Agent? {
        return agentPort.findById(agentId)
    }

    override fun getAllAgents(): List<Agent> {
        return agentPort.findAll()
    }

    override fun getAgentsByType(agentType: AgentType): List<Agent> {
        return agentPort.findByType(agentType)
    }

    override fun getHealthyAgents(): List<Agent> {
        return agentPort.findAll().filter { it.isHealthy() }
    }

    override fun markAgentOffline(agentId: String): Agent? {
        logger.info("Marking agent offline: $agentId")
        
        val agent = agentPort.findById(agentId) ?: return null
        val updated = agent.markOffline()
        
        return agentPort.save(updated).also {
            eventBroadcastService.broadcastAgentOffline(it)
        }
    }

    override fun deleteAgent(agentId: String): Boolean {
        logger.info("Deleting agent: $agentId")
        return agentPort.deleteById(agentId)
    }
}
