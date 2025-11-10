package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.DeploymentCommand
import io.github.architectplatform.server.application.domain.DeploymentOperation
import io.github.architectplatform.server.application.domain.DeploymentResult

/**
 * Use case interface for managing deployment commands.
 * Inbound port defining what the application can do with deployment commands.
 */
interface ManageDeploymentCommandUseCase {
    
    /**
     * Create a deployment command for a resource definition
     */
    fun createDeploymentCommand(
        resourceDefinitionId: String,
        agentId: String,
        operation: DeploymentOperation
    ): DeploymentCommand
    
    /**
     * Get pending commands for an agent
     */
    fun getPendingCommandsForAgent(agentId: String): List<DeploymentCommand>
    
    /**
     * Mark command as sent
     */
    fun markCommandSent(commandId: String): DeploymentCommand?
    
    /**
     * Record deployment result
     */
    fun recordDeploymentResult(
        commandId: String,
        result: DeploymentResult
    ): DeploymentCommand?
    
    /**
     * Get command by ID
     */
    fun getCommand(commandId: String): DeploymentCommand?
    
    /**
     * Get commands by agent
     */
    fun getCommandsByAgent(agentId: String): List<DeploymentCommand>
    
    /**
     * Get commands by resource definition
     */
    fun getCommandsByResourceDefinition(resourceDefinitionId: String): List<DeploymentCommand>
    
    /**
     * Cancel a pending command
     */
    fun cancelCommand(commandId: String): DeploymentCommand?
}
