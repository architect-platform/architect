package io.github.architectplatform.data.application.domain

import io.github.architectplatform.data.application.domain.ExecutionStatus.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for Execution domain model.
 * Tests business logic and state transitions.
 */
class ExecutionTest {
    
    @Test
    fun `should create execution with required fields`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = STARTED
        )
        
        assertEquals("exec-1", execution.id)
        assertEquals("proj-1", execution.projectId)
        assertEquals("engine-1", execution.engineId)
        assertEquals("build", execution.taskId)
        assertEquals(STARTED, execution.status)
        assertNull(execution.message)
        assertNull(execution.errorDetails)
        assertNotNull(execution.startedAt)
        assertNull(execution.completedAt)
    }
    
    @Test
    fun `complete should set status to COMPLETED and set completedAt`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = RUNNING
        )
        
        val completed = execution.complete("Build successful")
        
        assertEquals(COMPLETED, completed.status)
        assertEquals("Build successful", completed.message)
        assertNotNull(completed.completedAt)
        assertTrue(completed.completedAt!!.isAfter(execution.startedAt))
    }
    
    @Test
    fun `fail should set status to FAILED and capture error details`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = RUNNING
        )
        
        val failed = execution.fail("Compilation error", "Build failed")
        
        assertEquals(FAILED, failed.status)
        assertEquals("Compilation error", failed.errorDetails)
        assertEquals("Build failed", failed.message)
        assertNotNull(failed.completedAt)
    }
    
    @Test
    fun `updateStatus should update status and optionally message`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = STARTED
        )
        
        val running = execution.updateStatus(RUNNING, "Task executing")
        
        assertEquals(RUNNING, running.status)
        assertEquals("Task executing", running.message)
        assertNull(running.completedAt)
    }
    
    @Test
    fun `updateStatus with terminal status should set completedAt`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = RUNNING
        )
        
        val completed = execution.updateStatus(COMPLETED, "Done")
        
        assertEquals(COMPLETED, completed.status)
        assertNotNull(completed.completedAt)
    }
    
    @Test
    fun `updateStatus with non-terminal status should not set completedAt`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = STARTED
        )
        
        val running = execution.updateStatus(RUNNING)
        
        assertEquals(RUNNING, running.status)
        assertNull(running.completedAt)
    }
    
    @Test
    fun `execution should maintain immutability`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = STARTED
        )
        
        val completed = execution.complete()
        
        assertEquals(STARTED, execution.status)
        assertEquals(COMPLETED, completed.status)
        assertNull(execution.completedAt)
        assertNotNull(completed.completedAt)
    }
}
