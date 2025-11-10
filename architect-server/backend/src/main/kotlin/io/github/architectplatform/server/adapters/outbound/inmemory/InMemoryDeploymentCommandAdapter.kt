package io.github.architectplatform.server.adapters.outbound.inmemory

import io.github.architectplatform.server.application.domain.CommandStatus
import io.github.architectplatform.server.application.domain.DeploymentCommand
import io.github.architectplatform.server.application.ports.outbound.DeploymentCommandPort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of DeploymentCommandPort for development and testing.
 * Follows repository pattern with thread-safe operations.
 */
@Singleton
class InMemoryDeploymentCommandAdapter : DeploymentCommandPort {
    
    private val commands = ConcurrentHashMap<String, DeploymentCommand>()
    
    override fun save(command: DeploymentCommand): DeploymentCommand {
        commands[command.id] = command
        return command
    }
    
    override fun findById(id: String): DeploymentCommand? = commands[id]
    
    override fun findAll(): List<DeploymentCommand> = commands.values.toList()
    
    override fun findByAgentId(agentId: String): List<DeploymentCommand> {
        return commands.values.filter { it.agentId == agentId }
    }
    
    override fun findByResourceDefinitionId(resourceDefinitionId: String): List<DeploymentCommand> {
        return commands.values.filter { it.resourceDefinitionId == resourceDefinitionId }
    }
    
    override fun findByStatus(status: CommandStatus): List<DeploymentCommand> {
        return commands.values.filter { it.status == status }
    }
    
    override fun findByAgentIdAndStatus(agentId: String, status: CommandStatus): List<DeploymentCommand> {
        return commands.values.filter { it.agentId == agentId && it.status == status }
    }
    
    override fun deleteById(id: String): Boolean = commands.remove(id) != null
    
    override fun existsById(id: String): Boolean = commands.containsKey(id)
}
