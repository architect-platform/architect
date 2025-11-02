package io.github.architectplatform.engine.cloud

import io.github.architectplatform.engine.domain.events.*
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.event.ApplicationStartupEvent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.UUID

/**
 * Service that reports engine activities to the cloud backend.
 * 
 * This service:
 * - Generates a unique engine instance ID
 * - Registers the engine with the cloud on startup
 * - Reports all projects, executions, and events to the cloud
 * - Sends periodic heartbeats to maintain active status
 */
@Singleton
@Requires(property = "architect.cloud.enabled", value = "true")
class CloudReporterService(
    private val cloudClient: CloudClient,
    @Property(name = "micronaut.server.port", defaultValue = "9292")
    private val serverPort: Int,
    @Property(name = "architect.cloud.engine-id")
    private val configuredEngineId: String?
) : ApplicationEventListener<ApplicationStartupEvent> {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val engineId: String by lazy {
        configuredEngineId ?: UUID.randomUUID().toString()
    }
    
    private val hostname: String by lazy {
        try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    override fun onApplicationEvent(event: ApplicationStartupEvent) {
        registerEngine()
        startHeartbeat()
    }
    
    private fun registerEngine() {
        scope.launch {
            try {
                logger.info("Registering engine with cloud: $engineId")
                cloudClient.registerEngine(
                    RegisterEngineRequest(
                        id = engineId,
                        hostname = hostname,
                        port = serverPort,
                        version = this::class.java.`package`?.implementationVersion
                    )
                )
                logger.info("Engine registered successfully with cloud")
            } catch (e: Exception) {
                logger.warn("Failed to register engine with cloud: ${e.message}")
            }
        }
    }
    
    private fun startHeartbeat() {
        scope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(30_000) // 30 seconds
                    cloudClient.heartbeat(HeartbeatRequest(engineId))
                    logger.debug("Heartbeat sent to cloud")
                } catch (e: Exception) {
                    logger.warn("Failed to send heartbeat to cloud: ${e.message}")
                }
            }
        }
    }
    
    fun reportProject(projectName: String, projectPath: String, description: String? = null) {
        scope.launch {
            try {
                val projectId = generateProjectId(projectName, projectPath)
                cloudClient.registerProject(
                    RegisterProjectRequest(
                        id = projectId,
                        name = projectName,
                        path = projectPath,
                        engineId = engineId,
                        description = description
                    )
                )
                logger.debug("Reported project $projectName to cloud")
            } catch (e: Exception) {
                logger.warn("Failed to report project to cloud: ${e.message}")
            }
        }
    }
    
    fun reportExecution(executionId: String, projectName: String, taskId: String, status: String, message: String? = null, errorDetails: String? = null) {
        scope.launch {
            try {
                val projectId = generateProjectId(projectName, "")
                cloudClient.reportExecution(
                    ReportExecutionRequest(
                        id = executionId,
                        projectId = projectId,
                        engineId = engineId,
                        taskId = taskId,
                        status = status,
                        message = message,
                        errorDetails = errorDetails
                    )
                )
                logger.debug("Reported execution $executionId to cloud")
            } catch (e: Exception) {
                logger.warn("Failed to report execution to cloud: ${e.message}")
            }
        }
    }
    
    fun reportEvent(executionId: String, eventType: String, taskId: String? = null, message: String? = null, output: String? = null, success: Boolean = true) {
        scope.launch {
            try {
                cloudClient.reportEvent(
                    ReportEventRequest(
                        id = UUID.randomUUID().toString(),
                        executionId = executionId,
                        eventType = eventType,
                        taskId = taskId,
                        message = message,
                        output = output,
                        success = success
                    )
                )
                logger.trace("Reported event $eventType to cloud")
            } catch (e: Exception) {
                logger.warn("Failed to report event to cloud: ${e.message}")
            }
        }
    }
    
    private fun generateProjectId(projectName: String, @Suppress("UNUSED_PARAMETER") projectPath: String): String {
        return "$engineId-$projectName".replace(" ", "-").lowercase()
    }
}
