package io.github.architectplatform.server.adapters.inbound.rest

import io.github.architectplatform.server.adapters.inbound.rest.dto.AgentHeartbeatRequest
import io.github.architectplatform.server.adapters.inbound.rest.dto.AgentResponse
import io.github.architectplatform.server.adapters.inbound.rest.dto.RegisterAgentRequest
import io.github.architectplatform.server.application.ports.inbound.ManageAgentUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.slf4j.LoggerFactory

/**
 * REST controller for agent management.
 * Follows REST API best practices and clean architecture principles.
 * Inbound adapter that exposes agent use cases via HTTP.
 */
@Controller("/api/agents")
@ExecuteOn(TaskExecutors.IO)
class AgentController(
    private val manageAgentUseCase: ManageAgentUseCase
) {
    private val logger = LoggerFactory.getLogger(AgentController::class.java)

    @Post("/register")
    fun registerAgent(@Body request: RegisterAgentRequest): AgentResponse {
        logger.info("Registering agent: ${request.agentId}")
        
        val agent = manageAgentUseCase.registerAgent(
            agentId = request.agentId,
            agentType = request.agentType,
            namespace = request.namespace,
            capabilities = request.capabilities,
            metadata = request.metadata
        )
        
        return AgentResponse.fromDomain(agent)
    }

    @Post("/heartbeat")
    fun heartbeat(@Body request: AgentHeartbeatRequest): AgentResponse? {
        logger.debug("Heartbeat from agent: ${request.agentId}")
        
        val agent = manageAgentUseCase.recordHeartbeat(
            agentId = request.agentId,
            status = request.status,
            metrics = request.metrics
        )
        
        return agent?.let { AgentResponse.fromDomain(it) }
    }

    @Get
    fun getAllAgents(): List<AgentResponse> {
        return manageAgentUseCase.getAllAgents()
            .map { AgentResponse.fromDomain(it) }
    }

    @Get("/{agentId}")
    fun getAgent(@PathVariable agentId: String): AgentResponse? {
        return manageAgentUseCase.getAgent(agentId)
            ?.let { AgentResponse.fromDomain(it) }
    }

    @Get("/healthy")
    fun getHealthyAgents(): List<AgentResponse> {
        return manageAgentUseCase.getHealthyAgents()
            .map { AgentResponse.fromDomain(it) }
    }

    @Delete("/{agentId}")
    fun deleteAgent(@PathVariable agentId: String): Map<String, Boolean> {
        val deleted = manageAgentUseCase.deleteAgent(agentId)
        return mapOf("deleted" to deleted)
    }
}
