package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.ExecutionEvent
import io.github.architectplatform.data.application.ports.outbound.ExecutionEventPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Unit tests for ExecutionEventService.
 */
class ExecutionEventServiceTest {
    
    private lateinit var eventPort: ExecutionEventPort
    private lateinit var eventService: ExecutionEventService
    
    @BeforeEach
    fun setup() {
        eventPort = mockk()
        eventService = ExecutionEventService(eventPort)
    }
    
    @Test
    fun `recordEvent should save event`() {
        val event = ExecutionEvent(
            id = "event-1",
            executionId = "exec-1",
            eventType = "task.started",
            taskId = "build",
            message = "Task started"
        )
        
        every { eventPort.save(any()) } returns Mono.just(event)
        
        StepVerifier.create(eventService.recordEvent(event))
            .expectNext(event)
            .verifyComplete()
        
        verify { eventPort.save(event) }
    }
    
    @Test
    fun `getEventsByExecution should return all events for execution`() {
        val events = listOf(
            ExecutionEvent("event-1", "exec-1", "task.started"),
            ExecutionEvent("event-2", "exec-1", "task.completed")
        )
        
        every { eventPort.findByExecutionId("exec-1") } returns Flux.fromIterable(events)
        
        StepVerifier.create(eventService.getEventsByExecution("exec-1"))
            .expectNext(events[0])
            .expectNext(events[1])
            .verifyComplete()
    }
    
    @Test
    fun `getEventsByExecutionAndType should filter by type`() {
        val event = ExecutionEvent("event-1", "exec-1", "task.failed", success = false)
        
        every { 
            eventPort.findByExecutionIdAndEventType("exec-1", "task.failed") 
        } returns Flux.just(event)
        
        StepVerifier.create(eventService.getEventsByExecutionAndType("exec-1", "task.failed"))
            .expectNext(event)
            .verifyComplete()
        
        verify { eventPort.findByExecutionIdAndEventType("exec-1", "task.failed") }
    }
    
    @Test
    fun `deleteEventsByExecution should call port delete`() {
        every { eventPort.deleteByExecutionId("exec-1") } returns Mono.empty()
        
        StepVerifier.create(eventService.deleteEventsByExecution("exec-1"))
            .verifyComplete()
        
        verify(exactly = 1) { eventPort.deleteByExecutionId("exec-1") }
    }
}
