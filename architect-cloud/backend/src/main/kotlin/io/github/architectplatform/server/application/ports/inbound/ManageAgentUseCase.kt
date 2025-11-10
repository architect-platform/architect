package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.Agent
import io.github.architectplatform.server.application.domain.AgentStatus
import io.github.architectplatform.server.application.domain.AgentType

/**
 * Use case interface for managing agents.
 * Inbound port defining what the application can do with agents.
 */
interface ManageAgentUseCase {
    
    /**
     * Register a new agent
     */
    fun registerAgent(
        agentId: String,
        agentType: AgentType,
        namespace: String?,
        capabilities: List<String>,
        metadata: Map<String, String>
    ): Agent
    
    /**
     * Record agent heartbeat
     */
    fun recordHeartbeat(
        agentId: String,
        status: AgentStatus,
        metrics: Map<String, Any>
    ): Agent?
    
    /**
     * Get an agent by ID
     */
    fun getAgent(agentId: String): Agent?
    
    /**
     * Get all agents
     */
    fun getAllAgents(): List<Agent>
    
    /**
     * Get agents by type
     */
    fun getAgentsByType(agentType: AgentType): List<Agent>
    
    /**
     * Get healthy agents
     */
    fun getHealthyAgents(): List<Agent>
    
    /**
     * Mark agent as offline
     */
    fun markAgentOffline(agentId: String): Agent?
    
    /**
     * Delete an agent
     */
    fun deleteAgent(agentId: String): Boolean
}
