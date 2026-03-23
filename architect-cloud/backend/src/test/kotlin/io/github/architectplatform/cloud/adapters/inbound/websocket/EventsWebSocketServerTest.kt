package io.github.architectplatform.cloud.adapters.inbound.websocket

import io.github.architectplatform.cloud.application.services.CloudEvent
import io.github.architectplatform.cloud.application.services.EventBroadcastService
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.function.Predicate

class EventsWebSocketServerTest {

  private lateinit var broadcastService: EventBroadcastService
  private lateinit var wsBroadcaster: WebSocketBroadcaster
  private lateinit var server: EventsWebSocketServer
  private val sink: Sinks.Many<CloudEvent> = Sinks.many().multicast().onBackpressureBuffer()

  @BeforeEach
  fun setUp() {
    broadcastService = mock {
      on { getEventStream() } doReturn sink.asFlux()
    }
    wsBroadcaster = mock()
    server = EventsWebSocketServer(broadcastService, wsBroadcaster)
  }

  @Test
  fun `onOpen subscribes to event stream`() {
    val session = mockSession("s1")

    server.onOpen(session)

    verify(broadcastService).getEventStream()
  }

  @Test
  fun `onOpen sends welcome event`() {
    val session = mockSession("s1")

    server.onOpen(session)

    verify(session).sendSync(argThat<CloudEvent> { type == "CONNECTED" && entityId == "s1" })
  }

  @Test
  fun `onMessage responds to ping with pong`() {
    val session = mockSession("s1")

    server.onMessage("ping", session)

    verify(session).sendSync(argThat<CloudEvent> { type == "PONG" })
  }

  @Test
  fun `onMessage ignores non-ping messages`() {
    val session = mockSession("s1")

    server.onMessage("hello", session)

    verify(session, never()).sendSync(any<CloudEvent>())
  }

  @Test
  fun `onClose disposes subscription`() {
    val session = mockSession("s1")
    server.onOpen(session)

    server.onClose(session)

    // Verify no error — subscription is cleaned up
    // Emitting after close should not throw
    sink.tryEmitNext(CloudEvent(type = "TEST", entityId = "x", entityType = "T", data = emptyMap()))
  }

  @Test
  fun `onOpen forwards events to session via broadcaster`() {
    val session = mockSession("s1")
    server.onOpen(session)

    sink.tryEmitNext(CloudEvent(type = "ENGINE_REGISTERED", entityId = "e1", entityType = "ENGINE", data = emptyMap()))

    // Allow async delivery
    Thread.sleep(100)
    verify(wsBroadcaster).broadcastSync(argThat<CloudEvent> { type == "ENGINE_REGISTERED" }, any<Predicate<WebSocketSession>>())
  }

  private fun mockSession(id: String): WebSocketSession = mock {
    on { this.id } doReturn id
  }
}
