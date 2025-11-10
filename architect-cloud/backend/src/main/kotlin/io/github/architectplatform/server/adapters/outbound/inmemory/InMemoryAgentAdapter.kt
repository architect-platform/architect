package io.github.architectplatform.server.adapters.outbound.inmemory

import io.github.architectplatform.server.application.domain.Agent
import io.github.architectplatform.server.application.domain.AgentType
import io.github.architectplatform.server.application.ports.outbound.AgentPort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of AgentPort for development and testing.
 * Follows repository pattern with thread-safe operations.
 */
@Singleton
class InMemoryAgentAdapter : AgentPort {
    
    private val agents = ConcurrentHashMap<String, Agent>()
    
    override fun save(agent: Agent): Agent {
        agents[agent.id] = agent
        return agent
    }
    
    override fun findById(id: String): Agent? = agents[id]
    
    override fun findAll(): List<Agent> = agents.values.toList()
    
    override fun findByType(agentType: AgentType): List<Agent> {
        return agents.values.filter { it.agentType == agentType }
    }
    
    override fun deleteById(id: String): Boolean = agents.remove(id) != null
    
    override fun existsById(id: String): Boolean = agents.containsKey(id)
}
