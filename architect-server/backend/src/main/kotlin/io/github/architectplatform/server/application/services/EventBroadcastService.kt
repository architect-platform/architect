package io.github.architectplatform.server.application.services

import io.github.architectplatform.server.application.domain.*
import io.micronaut.serde.annotation.Serdeable
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Instant

/**
 * Service for broadcasting real-time events to connected clients.
 * Uses Project Reactor's Sinks for non-blocking event streaming.
 */
@Singleton
class EventBroadcastService {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    // Multi-cast sink for broadcasting to multiple subscribers
    private val sink: Sinks.Many<CloudEvent> = Sinks.many().multicast().onBackpressureBuffer()
    
    fun broadcast(event: CloudEvent) {
        logger.debug("Broadcasting event: ${event.type} - ${event.entityId}")
        sink.tryEmitNext(event)
    }
    
    fun getEventStream(): Flux<CloudEvent> {
        return sink.asFlux()
    }
    
    // Convenience methods for domain events
    
    fun broadcastEngineRegistered(engine: EngineInstance) {
        broadcast(CloudEvent(
            type = "ENGINE_REGISTERED",
            entityId = engine.id,
            entityType = "ENGINE",
            data = mapOf(
                "id" to engine.id,
                "hostname" to engine.hostname,
                "port" to engine.port,
                "status" to engine.status.name
            )
        ))
    }
    
    fun broadcastEngineHeartbeat(engineId: String) {
        broadcast(CloudEvent(
            type = "ENGINE_HEARTBEAT",
            entityId = engineId,
            entityType = "ENGINE",
            data = mapOf("id" to engineId)
        ))
    }
    
    fun broadcastProjectRegistered(project: Project) {
        broadcast(CloudEvent(
            type = "PROJECT_REGISTERED",
            entityId = project.id,
            entityType = "PROJECT",
            data = mapOf(
                "id" to project.id,
                "name" to project.name,
                "path" to project.path,
                "engineId" to project.engineId
            )
        ))
    }
    
    fun broadcastExecutionReported(execution: Execution) {
        broadcast(CloudEvent(
            type = "EXECUTION_${execution.status.name}",
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
    
    fun broadcastExecutionEvent(event: ExecutionEvent) {
        broadcast(CloudEvent(
            type = "EXECUTION_EVENT",
            entityId = event.id,
            entityType = "EVENT",
            data = mapOf(
                "id" to event.id,
                "executionId" to event.executionId,
                "eventType" to event.eventType,
                "taskId" to (event.taskId ?: ""),
                "success" to event.success,
                "message" to (event.message ?: "")
            )
        ))
    }
}

/**
 * Unified event structure for real-time broadcasting.
 */
@Serdeable
data class CloudEvent(
    val type: String,
    val entityId: String,
    val entityType: String,
    val data: Map<String, Any>,
    val timestamp: String = Instant.now().toString()
)
