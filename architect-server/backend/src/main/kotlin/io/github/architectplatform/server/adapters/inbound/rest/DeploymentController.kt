package io.github.architectplatform.server.adapters.inbound.rest

import io.github.architectplatform.server.adapters.inbound.rest.dto.DeploymentCommandResponse
import io.github.architectplatform.server.adapters.inbound.rest.dto.ReportDeploymentResultRequest
import io.github.architectplatform.server.application.ports.inbound.ManageDeploymentCommandUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * REST controller for deployment command management.
 * Handles command dispatch and result reporting from agents.
 */
@Controller("/api/deployments")
@ExecuteOn(TaskExecutors.IO)
class DeploymentController(
    private val manageDeploymentCommandUseCase: ManageDeploymentCommandUseCase
) {

    @Get("/{agentId}/commands")
    fun getPendingCommands(@PathVariable agentId: String): List<DeploymentCommandResponse> {
        val commands = manageDeploymentCommandUseCase.getPendingCommandsForAgent(agentId)
        
        // Mark commands as sent
        commands.forEach { command ->
            manageDeploymentCommandUseCase.markCommandSent(command.id)
        }
        
        return commands.map { DeploymentCommandResponse.fromDomain(it) }
    }

    @Post("/result")
    fun reportResult(@Body request: ReportDeploymentResultRequest): DeploymentCommandResponse? {
        val command = manageDeploymentCommandUseCase.recordDeploymentResult(
            commandId = request.commandId,
            result = request.result.toDomain()
        )
        return command?.let { DeploymentCommandResponse.fromDomain(it) }
    }

    @Get("/{commandId}")
    fun getCommand(@PathVariable commandId: String): DeploymentCommandResponse? {
        return manageDeploymentCommandUseCase.getCommand(commandId)
            ?.let { DeploymentCommandResponse.fromDomain(it) }
    }

    @Get("/agent/{agentId}")
    fun getCommandsByAgent(@PathVariable agentId: String): List<DeploymentCommandResponse> {
        return manageDeploymentCommandUseCase.getCommandsByAgent(agentId)
            .map { DeploymentCommandResponse.fromDomain(it) }
    }

    @Get("/resource/{resourceId}")
    fun getCommandsByResource(@PathVariable resourceId: String): List<DeploymentCommandResponse> {
        return manageDeploymentCommandUseCase.getCommandsByResourceDefinition(resourceId)
            .map { DeploymentCommandResponse.fromDomain(it) }
    }

    @Post("/{commandId}/cancel")
    fun cancelCommand(@PathVariable commandId: String): DeploymentCommandResponse? {
        return manageDeploymentCommandUseCase.cancelCommand(commandId)
            ?.let { DeploymentCommandResponse.fromDomain(it) }
    }
}
