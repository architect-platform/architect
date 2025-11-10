package io.github.architectplatform.agent.dockercompose.application

import io.github.architectplatform.agent.dockercompose.domain.AgentConfig
import io.github.architectplatform.agent.dockercompose.domain.DeploymentResult
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service for communication with Architect Server.
 * Applies Proxy Pattern - represents remote server locally.
 */
@Singleton
class ServerCommunicationService(
    @Client("\${agent.server.url}") private val httpClient: HttpClient,
    private val agentConfig: AgentConfig
) {

    private val logger = LoggerFactory.getLogger(ServerCommunicationService::class.java)

    fun registerAgent(): Boolean {
        return try {
            logger.info("Registering Docker Compose agent: ${agentConfig.agentId}")

            val request = HttpRequest.POST(
                "/api/agents/register",
                mapOf(
                    "agentId" to agentConfig.agentId,
                    "agentType" to "DOCKER",
                    "capabilities" to listOf("docker-compose", "template-rendering"),
                    "metadata" to mapOf(
                        "dockerComposeVersion" to getDockerComposeVersion()
                    )
                )
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            val response = httpClient.toBlocking().exchange(request, Map::class.java)
            logger.info("Agent registered successfully")
            true

        } catch (e: Exception) {
            logger.error("Failed to register agent: ${e.message}", e)
            false
        }
    }

    fun sendHeartbeat(): Boolean {
        return try {
            logger.debug("Sending heartbeat")

            val request = HttpRequest.POST(
                "/api/agents/heartbeat",
                mapOf(
                    "agentId" to agentConfig.agentId,
                    "status" to "HEALTHY"
                )
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            httpClient.toBlocking().exchange(request, Map::class.java)
            true

        } catch (e: Exception) {
            logger.error("Failed to send heartbeat: ${e.message}")
            false
        }
    }

    fun reportDeploymentResult(result: DeploymentResult): Boolean {
        return try {
            logger.info("Reporting deployment result for: ${result.commandId}")

            val request = HttpRequest.POST(
                "/api/deployments/result",
                mapOf(
                    "commandId" to result.commandId,
                    "result" to mapOf(
                        "success" to (result.status == io.github.architectplatform.agent.dockercompose.domain.DeploymentStatus.DEPLOYED),
                        "message" to result.message,
                        "appliedResources" to result.deployedServices.map {
                            mapOf(
                                "kind" to "Service",
                                "name" to it,
                                "namespace" to result.projectName,
                                "apiVersion" to "docker-compose/v1"
                            )
                        }
                    )
                )
            ).apply {
                agentConfig.serverToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            httpClient.toBlocking().exchange(request, Map::class.java)
            logger.info("Deployment result reported successfully")
            true

        } catch (e: Exception) {
            logger.error("Failed to report result: ${e.message}", e)
            false
        }
    }

    fun pollForCommands(): List<Map<String, Any>> {
        return try {
            val request = HttpRequest.GET<Map<String, Any>>(
                "/api/deployments/${agentConfig.agentId}/commands"
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

    private fun getDockerComposeVersion(): String {
        return try {
            val process = ProcessBuilder(agentConfig.dockerComposeCommand, "version", "--short")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "unknown"
        }
    }
}
