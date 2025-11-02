package io.github.architectplatform.cloud.application.ports.inbound

import io.github.architectplatform.cloud.application.domain.Execution
import io.github.architectplatform.cloud.application.domain.ExecutionStatus

/**
 * Inbound port for execution tracking use cases.
 * Defines operations that can be performed on executions from outside the application core.
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
}
