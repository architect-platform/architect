package io.github.architectplatform.server.adapters.inbound.websocket

import io.github.architectplatform.server.application.services.CloudEvent
import io.github.architectplatform.server.application.services.EventBroadcastService
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.slf4j.LoggerFactory
import reactor.core.Disposable

/**
 * WebSocket server for real-time event streaming.
 * Clients can connect to /ws/events to receive live updates.
 */
@ServerWebSocket("/ws/events")
class EventsWebSocketServer(
    private val eventBroadcastService: EventBroadcastService,
    private val broadcaster: WebSocketBroadcaster
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val subscriptions = mutableMapOf<String, Disposable>()
    
    @OnOpen
    fun onOpen(session: WebSocketSession) {
        val sessionId = session.id
        logger.info("WebSocket opened: $sessionId")
        
        // Subscribe to event stream for this session
        val subscription = eventBroadcastService.getEventStream()
            .subscribe { event ->
                try {
                    broadcaster.broadcastSync(event) { it.id == sessionId }
                } catch (e: Exception) {
                    logger.error("Error broadcasting to session $sessionId: ${e.message}")
                }
            }
        
        subscriptions[sessionId] = subscription
        
        // Send welcome message
        session.sendSync(CloudEvent(
            type = "CONNECTED",
            entityId = sessionId,
            entityType = "SESSION",
            data = mapOf("message" to "Connected to Architect Cloud event stream")
        ))
    }
    
    @OnMessage
    fun onMessage(message: String, session: WebSocketSession) {
        logger.debug("Received message from ${session.id}: $message")
        // Can handle ping/pong or other client messages here
        if (message == "ping") {
            session.sendSync(CloudEvent(
                type = "PONG",
                entityId = session.id,
                entityType = "PING",
                data = mapOf("message" to "pong")
            ))
        }
    }
    
    @OnClose
    fun onClose(session: WebSocketSession) {
        val sessionId = session.id
        logger.info("WebSocket closed: $sessionId")
        
        // Unsubscribe from event stream
        subscriptions[sessionId]?.dispose()
        subscriptions.remove(sessionId)
    }
}
