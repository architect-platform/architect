package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.Execution
import io.github.architectplatform.data.application.domain.ExecutionEvent
import io.github.architectplatform.data.application.domain.Project
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Instant

/**
 * Service for broadcasting real-time events to connected clients via WebSocket.
 * Uses Project Reactor for non-blocking event streaming.
 */
@Singleton
class EventBroadcastService {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    // Multicast sink for broadcasting events to all subscribers
    private val eventSink = Sinks.many().multicast().onBackpressureBuffer<DataEvent>()
    
    /**
     * Get a Flux stream of all events.
     * Multiple subscribers can consume this stream.
     */
    fun getEventStream(): Flux<DataEvent> {
        return eventSink.asFlux()
    }
    
    private fun broadcast(event: DataEvent) {
        val result = eventSink.tryEmitNext(event)
        if (result.isFailure) {
            logger.warn("Failed to broadcast event: ${event.type} - ${result}")
        } else {
            logger.debug("Broadcasted event: ${event.type} for ${event.entityType}: ${event.entityId}")
        }
    }
    
    // Convenience methods for domain events
    
    fun broadcastEngineRegistered(engine: EngineInstance) {
        broadcast(DataEvent(
            type = "ENGINE_REGISTERED",
            entityId = engine.id,
            entityType = "ENGINE",
            data = mapOf(
                "id" to engine.id,
                "hostname" to engine.hostname,
                "port" to engine.port,
                "status" to engine.status.name,
                "version" to (engine.version ?: "unknown")
            )
        ))
    }
    
    fun broadcastEngineHeartbeat(engineId: String) {
        broadcast(DataEvent(
            type = "ENGINE_HEARTBEAT",
            entityId = engineId,
            entityType = "ENGINE",
            data = mapOf("id" to engineId)
        ))
    }
    
    fun broadcastProjectRegistered(project: Project) {
        broadcast(DataEvent(
            type = "PROJECT_REGISTERED",
            entityId = project.id,
            entityType = "PROJECT",
            data = mapOf(
                "id" to project.id,
                "name" to project.name,
                "path" to project.path,
                "engineId" to project.engineId,
                "description" to (project.description ?: "")
            )
        ))
    }
    
    fun broadcastExecutionStarted(execution: Execution) {
        broadcast(DataEvent(
            type = "EXECUTION_STARTED",
            entityId = execution.id,
            entityType = "EXECUTION",
            data = mapOf(
                "id" to execution.id,
                "projectId" to execution.projectId,
                "engineId" to execution.engineId,
                "taskId" to execution.taskId,
                "status" to execution.status.name
            )
        ))
    }
    
    fun broadcastExecutionCompleted(execution: Execution) {
        broadcast(DataEvent(
            type = "EXECUTION_COMPLETED",
            entityId = execution.id,
            entityType = "EXECUTION",
            data = mapOf(
                "id" to execution.id,
                "projectId" to execution.projectId,
                "engineId" to execution.engineId,
                "taskId" to execution.taskId,
                "status" to execution.status.name,
                "message" to (execution.message ?: "")
            )
        ))
    }
    
    fun broadcastExecutionFailed(execution: Execution) {
        broadcast(DataEvent(
            type = "EXECUTION_FAILED",
            entityId = execution.id,
            entityType = "EXECUTION",
            data = mapOf(
                "id" to execution.id,
                "projectId" to execution.projectId,
                "engineId" to execution.engineId,
                "taskId" to execution.taskId,
                "status" to execution.status.name,
                "message" to (execution.message ?: ""),
                "errorDetails" to (execution.errorDetails ?: "")
            )
        ))
    }
    
    fun broadcastExecutionUpdated(execution: Execution) {
        broadcast(DataEvent(
            type = "EXECUTION_UPDATED",
            entityId = execution.id,
            entityType = "EXECUTION",
            data = mapOf(
                "id" to execution.id,
                "status" to execution.status.name,
                "message" to (execution.message ?: "")
            )
        ))
    }
    
    fun broadcastExecutionEvent(event: ExecutionEvent) {
        broadcast(DataEvent(
            type = "EXECUTION_EVENT",
            entityId = event.id,
            entityType = "EVENT",
            data = mapOf(
                "id" to event.id,
                "executionId" to event.executionId,
                "eventType" to event.eventType,
                "taskId" to (event.taskId ?: ""),
                "message" to (event.message ?: ""),
                "output" to (event.output ?: ""),
                "success" to event.success
            )
        ))
    }
}

/**
 * Represents a data event that occurred in the system.
 */
data class DataEvent(
    val type: String,
    val entityId: String,
    val entityType: String,
    val data: Map<String, Any>,
    val timestamp: Instant = Instant.now()
)
