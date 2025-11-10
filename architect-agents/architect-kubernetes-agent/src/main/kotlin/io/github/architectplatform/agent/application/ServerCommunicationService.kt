package io.github.architectplatform.agent.application

import io.github.architectplatform.agent.domain.AgentConfig
import io.github.architectplatform.agent.domain.DeploymentResult
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service responsible for communication with Architect Server.
 */
@Singleton
class ServerCommunicationService(
    @Client("\${agent.server.url}") private val httpClient: HttpClient,
    private val agentConfig: AgentConfig
) {

    private val logger = LoggerFactory.getLogger(ServerCommunicationService::class.java)

    /**
     * Register agent with Architect Server
     */
    fun registerAgent(): Boolean {
        return try {
            logger.info("Registering agent ${agentConfig.agentId} with server: ${agentConfig.serverUrl}")

            val request = HttpRequest.POST(
                "/api/agents/register",
                mapOf(
                    "agentId" to agentConfig.agentId,
                    "namespace" to agentConfig.kubernetesNamespace,
                    "capabilities" to listOf("kubernetes", "template-rendering")
                )
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.info("Agent registered successfully with status: ${response.status}")
            true

        } catch (e: Exception) {
            logger.error("Failed to register agent: ${e.message}", e)
            false
        }
    }

    /**
     * Send heartbeat to Architect Server
     */
    fun sendHeartbeat(): Boolean {
        return try {
            logger.debug("Sending heartbeat for agent: ${agentConfig.agentId}")

            val request = HttpRequest.POST(
                "/api/agents/heartbeat",
                mapOf(
                    "agentId" to agentConfig.agentId,
                    "timestamp" to System.currentTimeMillis()
                )
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.debug("Heartbeat sent successfully")
            true

        } catch (e: Exception) {
            logger.error("Failed to send heartbeat: ${e.message}")
            false
        }
    }

    /**
     * Report deployment result to Architect Server
     */
    fun reportDeploymentResult(result: DeploymentResult): Boolean {
        return try {
            logger.info("Reporting deployment result for command: ${result.commandId}")

            val request = HttpRequest.POST(
                "/api/deployments/result",
                mapOf(
                    "commandId" to result.commandId,
                    "resourceName" to result.resourceName,
                    "namespace" to result.namespace,
                    "status" to result.status.name,
                    "message" to result.message,
                    "appliedResources" to result.appliedResources.map {
                        mapOf(
                            "kind" to it.kind,
                            "name" to it.name,
                            "namespace" to it.namespace,
                            "apiVersion" to it.apiVersion
                        )
                    },
                    "completedAt" to result.completedAt?.toString()
                )
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.info("Deployment result reported successfully")
            true

        } catch (e: Exception) {
            logger.error("Failed to report deployment result: ${e.message}", e)
            false
        }
    }

    /**
     * Poll for new deployment commands from server
     */
    fun pollForCommands(): List<Map<String, Any>> {
        return try {
            logger.debug("Polling for deployment commands")

            val request = HttpRequest.GET<Map<String, Any>>(
                "/api/agents/${agentConfig.agentId}/commands"
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            val response = httpClient.toBlocking().retrieve(request, List::class.java)
            @Suppress("UNCHECKED_CAST")
            (response as? List<Map<String, Any>>) ?: emptyList()

        } catch (e: Exception) {
            logger.error("Failed to poll for commands: ${e.message}")
            emptyList()
        }
    }
}
