package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.Execution
import io.github.architectplatform.data.application.domain.ExecutionStatus
import io.github.architectplatform.data.application.ports.outbound.ExecutionPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Unit tests for ExecutionService.
 */
class ExecutionServiceTest {
    
    private lateinit var executionPort: ExecutionPort
    private lateinit var executionService: ExecutionService
    
    @BeforeEach
    fun setup() {
        executionPort = mockk()
        executionService = ExecutionService(executionPort)
    }
    
    @Test
    fun `startExecution should save new execution with STARTED status`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = ExecutionStatus.STARTED
        )
        
        every { executionPort.save(any()) } returns Mono.just(execution)
        
        StepVerifier.create(executionService.startExecution(execution))
            .expectNext(execution)
            .verifyComplete()
        
        verify { executionPort.save(execution) }
    }
    
    @Test
    fun `getExecution should return execution by id`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = ExecutionStatus.RUNNING
        )
        
        every { executionPort.findById("exec-1") } returns Mono.just(execution)
        
        StepVerifier.create(executionService.getExecution("exec-1"))
            .expectNext(execution)
            .verifyComplete()
    }
    
    @Test
    fun `getExecutionsByProject should return all executions for project`() {
        val executions = listOf(
            Execution("exec-1", "proj-1", "engine-1", "build", ExecutionStatus.COMPLETED),
            Execution("exec-2", "proj-1", "engine-1", "test", ExecutionStatus.RUNNING)
        )
        
        every { executionPort.findByProjectId("proj-1") } returns Flux.fromIterable(executions)
        
        StepVerifier.create(executionService.getExecutionsByProject("proj-1"))
            .expectNext(executions[0])
            .expectNext(executions[1])
            .verifyComplete()
    }
    
    @Test
    fun `getExecutionsByEngine should return all executions for engine`() {
        val executions = listOf(
            Execution("exec-1", "proj-1", "engine-1", "build", ExecutionStatus.COMPLETED)
        )
        
        every { executionPort.findByEngineId("engine-1") } returns Flux.fromIterable(executions)
        
        StepVerifier.create(executionService.getExecutionsByEngine("engine-1"))
            .expectNext(executions[0])
            .verifyComplete()
    }
    
    @Test
    fun `updateExecutionStatus should update status and save`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = ExecutionStatus.STARTED
        )
        
        val updated = execution.updateStatus(ExecutionStatus.RUNNING, "Building...")
        
        every { executionPort.findById("exec-1") } returns Mono.just(execution)
        every { executionPort.save(any()) } returns Mono.just(updated)
        
        StepVerifier.create(
            executionService.updateExecutionStatus("exec-1", ExecutionStatus.RUNNING, "Building...")
        )
            .expectNextMatches { 
                it.status == ExecutionStatus.RUNNING && it.message == "Building..." 
            }
            .verifyComplete()
        
        val savedSlot = slot<Execution>()
        verify { executionPort.save(capture(savedSlot)) }
        assertEquals(ExecutionStatus.RUNNING, savedSlot.captured.status)
        assertEquals("Building...", savedSlot.captured.message)
    }
    
    @Test
    fun `completeExecution should update to COMPLETED status`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = ExecutionStatus.RUNNING
        )
        
        val completed = execution.complete("Build successful")
        
        every { executionPort.findById("exec-1") } returns Mono.just(execution)
        every { executionPort.save(any()) } returns Mono.just(completed)
        
        StepVerifier.create(executionService.completeExecution("exec-1", "Build successful"))
            .expectNextMatches { 
                it.status == ExecutionStatus.COMPLETED && 
                it.message == "Build successful" &&
                it.completedAt != null
            }
            .verifyComplete()
    }
    
    @Test
    fun `failExecution should update to FAILED status with error details`() {
        val execution = Execution(
            id = "exec-1",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = ExecutionStatus.RUNNING
        )
        
        val failed = execution.fail("Compilation error", "Build failed")
        
        every { executionPort.findById("exec-1") } returns Mono.just(execution)
        every { executionPort.save(any()) } returns Mono.just(failed)
        
        StepVerifier.create(
            executionService.failExecution("exec-1", "Compilation error", "Build failed")
        )
            .expectNextMatches { 
                it.status == ExecutionStatus.FAILED && 
                it.errorDetails == "Compilation error" &&
                it.message == "Build failed" &&
                it.completedAt != null
            }
            .verifyComplete()
    }
    
    @Test
    fun `deleteExecution should call port delete`() {
        every { executionPort.deleteById("exec-1") } returns Mono.empty()
        
        StepVerifier.create(executionService.deleteExecution("exec-1"))
            .verifyComplete()
        
        verify(exactly = 1) { executionPort.deleteById("exec-1") }
    }
}
