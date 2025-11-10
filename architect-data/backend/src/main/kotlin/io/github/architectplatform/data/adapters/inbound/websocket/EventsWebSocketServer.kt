package io.github.architectplatform.data.adapters.inbound.websocket

import io.github.architectplatform.data.application.services.DataEvent
import io.github.architectplatform.data.application.services.EventBroadcastService
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.slf4j.LoggerFactory
import reactor.core.Disposable

/**
 * WebSocket server for streaming real-time data events to clients.
 * Clients connect to /ws/events and receive live updates.
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
        logger.info("WebSocket connection opened: ${session.id}")
        
        // Subscribe to event stream and broadcast to this client
        val subscription = eventBroadcastService.getEventStream()
            .subscribe(
                { event -> sendEventToSession(session, event) },
                { error -> logger.error("Error in event stream for session ${session.id}", error) },
                { logger.info("Event stream completed for session ${session.id}") }
            )
        
        subscriptions[session.id] = subscription
    }
    
    @OnMessage
    fun onMessage(message: String, session: WebSocketSession) {
        logger.debug("Received message from ${session.id}: $message")
        // Echo back for connection testing
        session.sendSync("""{"type":"pong","message":"$message"}""")
    }
    
    @OnClose
    fun onClose(session: WebSocketSession) {
        logger.info("WebSocket connection closed: ${session.id}")
        subscriptions[session.id]?.dispose()
        subscriptions.remove(session.id)
    }
    
    private fun sendEventToSession(session: WebSocketSession, event: DataEvent) {
        try {
            if (session.isOpen) {
                val json = createEventJson(event)
                session.sendAsync(json)
            }
        } catch (e: Exception) {
            logger.warn("Failed to send event to session ${session.id}: ${e.message}")
        }
    }
    
    private fun createEventJson(event: DataEvent): String {
        val dataJson = event.data.entries.joinToString(",") { (key, value) ->
            """"$key":"${value.toString().replace("\"", "\\\"")}""""
        }
        
        return """
            {
                "type":"${event.type}",
                "entityId":"${event.entityId}",
                "entityType":"${event.entityType}",
                "data":{$dataJson},
                "timestamp":"${event.timestamp}"
            }
        """.trimIndent().replace("\n", "")
    }
}
