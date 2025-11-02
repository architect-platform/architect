package io.github.architectplatform.engine.cloud

import io.github.architectplatform.engine.domain.events.*
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Listener that automatically reports all execution events to the cloud.
 */
@Singleton
@Requires(property = "architect.cloud.enabled", value = "true")
class CloudEventListener(
    private val cloudReporter: CloudReporterService
) : ApplicationEventListener<ArchitectEvent<*>> {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun onApplicationEvent(event: ArchitectEvent<*>) {
        try {
            when (val eventData = event.event) {
                is ExecutionEvent -> handleExecutionEvent(eventData)
                is ExecutionTaskEvent -> handleTaskEvent(eventData)
                else -> logger.trace("Ignoring non-execution event: ${event.id}")
            }
        } catch (e: Exception) {
            logger.warn("Failed to process event for cloud reporting: ${e.message}")
        }
    }
    
    private fun handleExecutionEvent(event: ExecutionEvent) {
        val status = when (event.executionEventType) {
            ExecutionEventType.STARTED -> "STARTED"
            ExecutionEventType.COMPLETED -> "COMPLETED"
            ExecutionEventType.FAILED -> "FAILED"
            ExecutionEventType.SKIPPED -> "SKIPPED"
            else -> return
        }
        
        cloudReporter.reportExecution(
            executionId = event.executionId,
            projectName = event.project,
            taskId = "workflow", // Execution-level events don't have specific task
            status = status,
            message = event.message,
            errorDetails = event.errorDetails
        )
        
        cloudReporter.reportEvent(
            executionId = event.executionId,
            eventType = event.executionEventType.name.lowercase(),
            message = event.message,
            success = event.success
        )
    }
    
    private fun handleTaskEvent(event: ExecutionTaskEvent) {
        val status = when (event.executionEventType) {
            ExecutionEventType.STARTED -> "STARTED"
            ExecutionEventType.TASK_COMPLETED -> "COMPLETED"
            ExecutionEventType.FAILED -> "FAILED"
            ExecutionEventType.SKIPPED -> "SKIPPED"
            ExecutionEventType.OUTPUT -> null // Don't update execution status for output
            else -> null
        }
        
        if (status != null) {
            cloudReporter.reportExecution(
                executionId = event.executionId,
                projectName = event.project,
                taskId = event.taskId,
                status = status,
                message = event.message,
                errorDetails = event.errorDetails
            )
        }
        
        cloudReporter.reportEvent(
            executionId = event.executionId,
            eventType = event.executionEventType.name.lowercase(),
            taskId = event.taskId,
            message = event.message,
            output = if (event.executionEventType == ExecutionEventType.OUTPUT) event.message else null,
            success = event.success
        )
    }
}
