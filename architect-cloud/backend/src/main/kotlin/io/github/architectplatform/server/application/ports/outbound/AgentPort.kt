package io.github.architectplatform.server.application.ports.outbound

import io.github.architectplatform.server.application.domain.Agent
import io.github.architectplatform.server.application.domain.AgentType

/**
 * Outbound port for agent persistence.
 * Defines what the application needs from agent storage.
 */
interface AgentPort {
    
    fun save(agent: Agent): Agent
    
    fun findById(id: String): Agent?
    
    fun findAll(): List<Agent>
    
    fun findByType(agentType: AgentType): List<Agent>
    
    fun deleteById(id: String): Boolean
    
    fun existsById(id: String): Boolean
}
