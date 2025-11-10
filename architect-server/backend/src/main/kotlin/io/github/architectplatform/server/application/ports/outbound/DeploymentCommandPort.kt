package io.github.architectplatform.server.application.ports.outbound

import io.github.architectplatform.server.application.domain.CommandStatus
import io.github.architectplatform.server.application.domain.DeploymentCommand

/**
 * Outbound port for deployment command persistence.
 * Defines what the application needs from deployment command storage.
 */
interface DeploymentCommandPort {
    
    fun save(command: DeploymentCommand): DeploymentCommand
    
    fun findById(id: String): DeploymentCommand?
    
    fun findAll(): List<DeploymentCommand>
    
    fun findByAgentId(agentId: String): List<DeploymentCommand>
    
    fun findByResourceDefinitionId(resourceDefinitionId: String): List<DeploymentCommand>
    
    fun findByStatus(status: CommandStatus): List<DeploymentCommand>
    
    fun findByAgentIdAndStatus(agentId: String, status: CommandStatus): List<DeploymentCommand>
    
    fun deleteById(id: String): Boolean
    
    fun existsById(id: String): Boolean
}
