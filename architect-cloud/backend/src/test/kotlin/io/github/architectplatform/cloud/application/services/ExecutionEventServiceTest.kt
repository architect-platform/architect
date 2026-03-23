package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.ExecutionEvent
import io.github.architectplatform.cloud.application.ports.outbound.ExecutionEventPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ExecutionEventServiceTest {

  private lateinit var port: InMemoryExecutionEventPort
  private lateinit var broadcast: EventBroadcastService
  private lateinit var service: ExecutionEventService

  @BeforeEach
  fun setUp() {
    port = InMemoryExecutionEventPort()
    broadcast = mock()
    service = ExecutionEventService(port, broadcast)
  }

  @Test
  fun `reportEvent creates and saves event`() {
    val event = service.reportEvent("ev1", "x1", "TASK_STARTED", "build", "starting", null, true)

    assertEquals("ev1", event.id)
    assertEquals("x1", event.executionId)
    assertEquals("TASK_STARTED", event.eventType)
    assertEquals("build", event.taskId)
    assertEquals("starting", event.message)
    assertTrue(event.success)
    assertNotNull(port.findByExecutionId("x1").find { it.id == "ev1" })
  }

  @Test
  fun `reportEvent with failure`() {
    val event = service.reportEvent("ev1", "x1", "TASK_FAILED", "build", "error", "stack trace", false)

    assertFalse(event.success)
    assertEquals("stack trace", event.output)
  }

  @Test
  fun `reportEvent broadcasts execution event`() {
    service.reportEvent("ev1", "x1", "TASK_STARTED", "build", null, null, true)

    verify(broadcast).broadcastExecutionEvent(any())
  }

  @Test
  fun `reportEvent with null optional fields`() {
    val event = service.reportEvent("ev1", "x1", "LOG", null, null, null, true)

    assertNull(event.taskId)
    assertNull(event.message)
    assertNull(event.output)
  }

  @Test
  fun `getExecutionEvents returns events for execution`() {
    port.save(ExecutionEvent(id = "ev1", executionId = "x1", eventType = "STARTED"))
    port.save(ExecutionEvent(id = "ev2", executionId = "x1", eventType = "COMPLETED"))
    port.save(ExecutionEvent(id = "ev3", executionId = "x2", eventType = "STARTED"))

    val result = service.getExecutionEvents("x1")

    assertEquals(2, result.size)
    assertTrue(result.all { it.executionId == "x1" })
  }

  @Test
  fun `getExecutionEvents returns empty list when no events`() {
    val result = service.getExecutionEvents("nonexistent")

    assertTrue(result.isEmpty())
  }

  private class InMemoryExecutionEventPort : ExecutionEventPort {
    private val store = mutableListOf<ExecutionEvent>()

    override fun save(event: ExecutionEvent): ExecutionEvent {
      store.add(event)
      return event
    }

    override fun findByExecutionId(executionId: String) = store.filter { it.executionId == executionId }
  }
}
