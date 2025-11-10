package io.github.architectplatform.data.application.ports.inbound

import io.github.architectplatform.data.application.domain.Execution
import io.github.architectplatform.data.application.domain.ExecutionStatus

/**
 * Inbound port for execution tracking use cases.
 * Defines operations for monitoring workflow executions.
 */
interface TrackExecutionUseCase {
    fun reportExecution(
        id: String,
        projectId: String,
        engineId: String,
        taskId: String,
        status: ExecutionStatus,
        message: String? = null,
        errorDetails: String? = null
    ): Execution
    
    fun getExecution(executionId: String): Execution?
    fun getExecutionsByProject(projectId: String): List<Execution>
    fun getExecutionsByEngine(engineId: String): List<Execution>
    fun getRecentExecutions(limit: Int = 50): List<Execution>
}
