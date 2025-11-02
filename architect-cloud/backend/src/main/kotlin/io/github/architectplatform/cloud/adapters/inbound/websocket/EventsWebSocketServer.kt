package io.github.architectplatform.cloud.adapters.inbound.websocket

import io.github.architectplatform.cloud.application.services.CloudEvent
import io.github.architectplatform.cloud.application.services.EventBroadcastService
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
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
                runBlocking {
                    try {
                        broadcaster.broadcastAsync(event, { it.id == sessionId }).awaitFirstOrNull()
                    } catch (e: Exception) {
                        logger.error("Error broadcasting to session $sessionId: ${e.message}")
                    }
                }
            }
        
        subscriptions[sessionId] = subscription
        
        // Send welcome message
        runBlocking {
            session.sendAsync(CloudEvent(
                type = "CONNECTED",
                entityId = sessionId,
                entityType = "SESSION",
                data = mapOf("message" to "Connected to Architect Cloud event stream")
            )).awaitFirstOrNull()
        }
    }
    
    @OnMessage
    fun onMessage(message: String, session: WebSocketSession) {
        logger.debug("Received message from ${session.id}: $message")
        // Can handle ping/pong or other client messages here
        if (message == "ping") {
            runBlocking {
                session.sendAsync(CloudEvent(
                    type = "PONG",
                    entityId = session.id,
                    entityType = "PING",
                    data = mapOf("message" to "pong")
                )).awaitFirstOrNull()
            }
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
