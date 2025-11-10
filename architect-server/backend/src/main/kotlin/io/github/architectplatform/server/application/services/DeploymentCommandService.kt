package io.github.architectplatform.server.application.services

import io.github.architectplatform.server.application.domain.CommandStatus
import io.github.architectplatform.server.application.domain.DeploymentCommand
import io.github.architectplatform.server.application.domain.DeploymentOperation
import io.github.architectplatform.server.application.domain.DeploymentResult
import io.github.architectplatform.server.application.ports.inbound.ManageDeploymentCommandUseCase
import io.github.architectplatform.server.application.ports.outbound.AgentPort
import io.github.architectplatform.server.application.ports.outbound.DeploymentCommandPort
import io.github.architectplatform.server.application.ports.outbound.ApplicationDefinitionPort
import io.github.architectplatform.server.application.ports.outbound.TemplatePort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Service implementing deployment command management use cases.
 * Follows single responsibility principle - handles deployment command operations.
 * Applies dependency inversion principle - depends on abstractions (ports).
 */
@Singleton
class DeploymentCommandService(
    private val deploymentCommandPort: DeploymentCommandPort,
    private val resourceDefinitionPort: ApplicationDefinitionPort,
    private val templatePort: TemplatePort,
    private val agentPort: AgentPort,
    private val eventBroadcastService: EventBroadcastService
) : ManageDeploymentCommandUseCase {

    private val logger = LoggerFactory.getLogger(DeploymentCommandService::class.java)

    override fun createDeploymentCommand(
        resourceDefinitionId: String,
        agentId: String,
        operation: DeploymentOperation
    ): DeploymentCommand {
        logger.info("Creating deployment command for resource: $resourceDefinitionId, agent: $agentId")
        
        // Validate resource definition exists
        val resourceDef = resourceDefinitionPort.findById(resourceDefinitionId)
            ?: throw IllegalArgumentException("Resource definition not found: $resourceDefinitionId")
        
        // Validate agent exists and is healthy
        val agent = agentPort.findById(agentId)
            ?: throw IllegalArgumentException("Agent not found: $agentId")
        
        if (!agent.isHealthy()) {
            throw IllegalStateException("Agent is not healthy: $agentId")
        }
        
        // Get templates
        val templates = resourceDef.templateIds.mapNotNull { templatePort.findById(it) }
        if (templates.isEmpty()) {
            throw IllegalStateException("No templates found for resource definition")
        }
        
        // Create command
        val command = DeploymentCommand(
            id = UUID.randomUUID().toString(),
            agentId = agentId,
            applicationDefinitionId = resourceDefinitionId,
            applicationName = resourceDef.name,
            templates = templates.map { it.content },
            variables = resourceDef.toVariableMap(),
            dependencies = resourceDef.getDependenciesInOrder(),
            operation = operation
        )
        
        return deploymentCommandPort.save(command).also {
            eventBroadcastService.broadcastDeploymentCommandCreated(it)
            logger.info("Deployment command created: ${it.id}")
        }
    }

    override fun getPendingCommandsForAgent(agentId: String): List<DeploymentCommand> {
        return deploymentCommandPort.findByAgentIdAndStatus(agentId, CommandStatus.PENDING)
    }

    override fun markCommandSent(commandId: String): DeploymentCommand? {
        logger.info("Marking command as sent: $commandId")
        
        val command = deploymentCommandPort.findById(commandId) ?: run {
            logger.warn("Command not found: $commandId")
            return null
        }
        
        val updated = command.markSent()
        return deploymentCommandPort.save(updated)
    }

    override fun recordDeploymentResult(
        commandId: String,
        result: DeploymentResult
    ): DeploymentCommand? {
        logger.info("Recording deployment result for command: $commandId, success: ${result.success}")
        
        val command = deploymentCommandPort.findById(commandId) ?: run {
            logger.warn("Command not found: $commandId")
            return null
        }
        
        val updated = command.markCompleted(result)
        return deploymentCommandPort.save(updated).also {
            eventBroadcastService.broadcastDeploymentCompleted(it)
        }
    }

    override fun getCommand(commandId: String): DeploymentCommand? {
        return deploymentCommandPort.findById(commandId)
    }

    override fun getCommandsByAgent(agentId: String): List<DeploymentCommand> {
        return deploymentCommandPort.findByAgentId(agentId)
    }

    override fun getCommandsByApplicationDefinition(resourceDefinitionId: String): List<DeploymentCommand> {
        return deploymentCommandPort.findByApplicationDefinitionId(resourceDefinitionId)
    }

    override fun cancelCommand(commandId: String): DeploymentCommand? {
        logger.info("Cancelling command: $commandId")
        
        val command = deploymentCommandPort.findById(commandId) ?: return null
        
        if (command.status != CommandStatus.PENDING) {
            logger.warn("Cannot cancel command in status: ${command.status}")
            return null
        }
        
        val cancelled = command.copy(status = CommandStatus.CANCELLED)
        return deploymentCommandPort.save(cancelled)
    }
}
