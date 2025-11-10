package io.github.architectplatform.agent.common.service

import io.github.architectplatform.agent.common.domain.AgentConfig
import io.github.architectplatform.agent.common.domain.DeploymentCommand
import io.github.architectplatform.agent.common.domain.DeploymentResult
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Common service for communicating with the Architect Cloud backend.
 * Handles registration, heartbeats, and command polling.
 * Shared across all agent types.
 */
@Singleton
class ServerCommunicationService(
    @Client("\${agent.server.url}")
    private val httpClient: HttpClient,
    private val config: AgentConfig
) {
    
    private val logger = LoggerFactory.getLogger(ServerCommunicationService::class.java)
    
    /**
     * Register the agent with the cloud backend.
     */
    fun registerAgent(): Boolean {
        return try {
            val request = HttpRequest.POST("/api/agents", mapOf(
                "id" to config.id,
                "agentType" to config.agentType.name,
                "supportedEnvironments" to config.supportedEnvironments,
                "cloudProvider" to config.cloudProvider,
                "region" to config.region,
                "capabilities" to config.capabilities,
                "metadata" to config.metadata
            ))
            
            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.info("Agent registered successfully: ${config.id}")
            response.status.code in 200..299
        } catch (e: Exception) {
            logger.error("Failed to register agent: ${e.message}", e)
            false
        }
    }
    
    /**
     * Send heartbeat to the cloud backend.
     */
    fun sendHeartbeat(): Boolean {
        return try {
            val request = HttpRequest.POST("/api/agents/heartbeat", mapOf(
                "agentId" to config.id
            ))
            
            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.debug("Heartbeat sent successfully")
            response.status.code in 200..299
        } catch (e: Exception) {
            logger.warn("Failed to send heartbeat: ${e.message}")
            false
        }
    }
    
    /**
     * Report deployment result back to the cloud backend.
     */
    fun reportDeploymentResult(result: DeploymentResult): Boolean {
        return try {
            val request = HttpRequest.POST("/api/deployments/result", mapOf(
                "commandId" to result.commandId,
                "success" to result.success,
                "message" to result.message,
                "appliedResources" to result.appliedResources.map { resource ->
                    mapOf(
                        "kind" to resource.kind,
                        "name" to resource.name,
                        "namespace" to resource.namespace,
                        "status" to resource.status
                    )
                },
                "error" to result.error,
                "deploymentUrl" to result.deploymentUrl,
                "healthStatus" to result.healthStatus
            ))
            
            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.info("Deployment result reported successfully for command: ${result.commandId}")
            response.status.code in 200..299
        } catch (e: Exception) {
            logger.error("Failed to report deployment result: ${e.message}", e)
            false
        }
    }
    
    /**
     * Poll for pending deployment commands.
     */
    fun pollDeploymentCommands(): List<DeploymentCommand> {
        return try {
            val request = HttpRequest.GET<List<Map<String, Any>>>("/api/deployments/pending?agentId=${config.id}")
            val response = httpClient.toBlocking().retrieve(request, List::class.java)
            
            @Suppress("UNCHECKED_CAST")
            (response as List<Map<String, Any>>).map { commandMap ->
                DeploymentCommand(
                    id = commandMap["id"] as String,
                    agentId = commandMap["agentId"] as String,
                    applicationDefinitionId = commandMap["applicationDefinitionId"] as String,
                    applicationName = commandMap["applicationName"] as String,
                    resourceName = commandMap["resourceName"] as? String ?: commandMap["applicationName"] as String,
                    templates = (commandMap["templates"] as List<*>).map { it.toString() },
                    variables = commandMap["variables"] as Map<String, Any>,
                    targetEnvironment = commandMap["targetEnvironment"] as? String ?: "development",
                    tags = commandMap["tags"] as? Map<String, String> ?: emptyMap()
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to poll deployment commands: ${e.message}", e)
            emptyList()
        }
    }
}
