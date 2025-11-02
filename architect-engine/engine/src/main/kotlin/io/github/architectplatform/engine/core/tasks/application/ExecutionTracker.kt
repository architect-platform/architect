package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.engine.core.tasks.interfaces.dto.ExecutionStatus
import io.github.architectplatform.engine.core.tasks.interfaces.dto.ExecutionStatusDTO
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionEventType
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that tracks execution status and statistics.
 * 
 * Listens to execution events and maintains execution state for querying.
 */
@Singleton
class ExecutionTracker {
    
    private val executions = ConcurrentHashMap<ExecutionId, ExecutionStatusDTO>()
    
    /**
     * Listen to execution events and update tracking state.
     */
    @EventListener
    fun onExecutionEvent(event: ArchitectEvent<*>) {
        if (event.event !is ExecutionEvent) return
        
        val executionEvent = event.event as ExecutionEvent
        val executionId = executionEvent.executionId
        val projectName = executionEvent.project
        
        when (event.id) {
            "execution.started" -> {
                executions[executionId] = ExecutionStatusDTO(
                    executionId = executionId,
                    projectName = projectName,
                    taskId = "unknown", // Will be updated with task events
                    status = ExecutionStatus.RUNNING,
                    startTime = System.currentTimeMillis()
                )
            }
            "task.started" -> {
                val current = executions[executionId]
                if (current != null) {
                    executions[executionId] = current.copy(
                        totalTasks = current.totalTasks + 1
                    )
                }
            }
            "task.completed" -> {
                val current = executions[executionId]
                if (current != null) {
                    executions[executionId] = current.copy(
                        completedTasks = current.completedTasks + 1
                    )
                }
            }
            "task.failed" -> {
                val current = executions[executionId]
                if (current != null) {
                    executions[executionId] = current.copy(
                        failedTasks = current.failedTasks + 1
                    )
                }
            }
            "task.skipped" -> {
                val current = executions[executionId]
                if (current != null) {
                    executions[executionId] = current.copy(
                        skippedTasks = current.skippedTasks + 1
                    )
                }
            }
            "execution.completed" -> {
                val current = executions[executionId]
                if (current != null) {
                    executions[executionId] = current.copy(
                        status = ExecutionStatus.COMPLETED,
                        endTime = System.currentTimeMillis()
                    )
                }
            }
            "execution.failed" -> {
                val current = executions[executionId]
                if (current != null) {
                    executions[executionId] = current.copy(
                        status = ExecutionStatus.FAILED,
                        endTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }
    
    /**
     * Get the status of a specific execution.
     * 
     * @param executionId The execution ID to query
     * @return The execution status, or null if not found
     */
    fun getExecutionStatus(executionId: ExecutionId): ExecutionStatusDTO? {
        return executions[executionId]
    }
    
    /**
     * Get all tracked executions.
     * 
     * @return List of all execution statuses
     */
    fun getAllExecutions(): List<ExecutionStatusDTO> {
        return executions.values.toList()
    }
}
