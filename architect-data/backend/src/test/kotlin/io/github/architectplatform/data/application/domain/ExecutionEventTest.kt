package io.github.architectplatform.data.application.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ExecutionEvent domain model.
 */
class ExecutionEventTest {
    
    @Test
    fun `should create execution event with all fields`() {
        val event = ExecutionEvent(
            id = "event-1",
            executionId = "exec-1",
            eventType = "task.started",
            taskId = "build",
            message = "Task started",
            output = "Building...",
            success = true
        )
        
        assertEquals("event-1", event.id)
        assertEquals("exec-1", event.executionId)
        assertEquals("task.started", event.eventType)
        assertEquals("build", event.taskId)
        assertEquals("Task started", event.message)
        assertEquals("Building...", event.output)
        assertTrue(event.success)
        assertNotNull(event.timestamp)
    }
    
    @Test
    fun `should create event with minimal fields`() {
        val event = ExecutionEvent(
            id = "event-1",
            executionId = "exec-1",
            eventType = "task.completed"
        )
        
        assertEquals("event-1", event.id)
        assertEquals("exec-1", event.executionId)
        assertEquals("task.completed", event.eventType)
        assertNull(event.taskId)
        assertNull(event.message)
        assertNull(event.output)
        assertTrue(event.success)
    }
    
    @Test
    fun `should create failure event`() {
        val event = ExecutionEvent(
            id = "event-1",
            executionId = "exec-1",
            eventType = "task.failed",
            taskId = "build",
            message = "Build failed",
            output = "Error: compilation failed",
            success = false
        )
        
        assertEquals("task.failed", event.eventType)
        assertFalse(event.success)
        assertEquals("Build failed", event.message)
    }
    
    @Test
    fun `event should be immutable`() {
        val event = ExecutionEvent(
            id = "event-1",
            executionId = "exec-1",
            eventType = "task.started"
        )
        
        val updated = event.copy(message = "Updated message")
        
        assertNull(event.message)
        assertEquals("Updated message", updated.message)
        assertEquals(event.id, updated.id)
    }
}
