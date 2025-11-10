package io.github.architectplatform.data.adapters.inbound.rest.dto

import io.github.architectplatform.data.application.domain.*
import io.micronaut.serde.annotation.Serdeable

// Engine DTOs
@Serdeable
data class RegisterEngineRequest(
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?
)

@Serdeable
data class HeartbeatRequest(
    val engineId: String
)

@Serdeable
data class EngineInstanceResponse(
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?,
    val status: String,
    val createdAt: String,
    val lastHeartbeat: String
) {
    companion object {
        fun fromDomain(engine: EngineInstance): EngineInstanceResponse {
            return EngineInstanceResponse(
                id = engine.id,
                hostname = engine.hostname,
                port = engine.port,
                version = engine.version,
                status = engine.status.name,
                createdAt = engine.createdAt.toString(),
                lastHeartbeat = engine.lastHeartbeat.toString()
            )
        }
    }
}

// Project DTOs
@Serdeable
data class RegisterProjectRequest(
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String? = null
)

@Serdeable
data class ProjectResponse(
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String?,
    val createdAt: String
) {
    companion object {
        fun fromDomain(project: Project): ProjectResponse {
            return ProjectResponse(
                id = project.id,
                name = project.name,
                path = project.path,
                engineId = project.engineId,
                description = project.description,
                createdAt = project.createdAt.toString()
            )
        }
    }
}

// Execution DTOs
@Serdeable
data class ReportExecutionRequest(
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: String,
    val message: String? = null,
    val errorDetails: String? = null
)

@Serdeable
data class ExecutionResponse(
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: String,
    val message: String?,
    val errorDetails: String?,
    val startedAt: String,
    val completedAt: String?
) {
    companion object {
        fun fromDomain(execution: Execution): ExecutionResponse {
            return ExecutionResponse(
                id = execution.id,
                projectId = execution.projectId,
                engineId = execution.engineId,
                taskId = execution.taskId,
                status = execution.status.name,
                message = execution.message,
                errorDetails = execution.errorDetails,
                startedAt = execution.startedAt.toString(),
                completedAt = execution.completedAt?.toString()
            )
        }
    }
}

// Execution Event DTOs
@Serdeable
data class ReportEventRequest(
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String? = null,
    val message: String? = null,
    val output: String? = null,
    val success: Boolean = true
)

@Serdeable
data class ExecutionEventResponse(
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String?,
    val message: String?,
    val output: String?,
    val success: Boolean,
    val timestamp: String
) {
    companion object {
        fun fromDomain(event: ExecutionEvent): ExecutionEventResponse {
            return ExecutionEventResponse(
                id = event.id,
                executionId = event.executionId,
                eventType = event.eventType,
                taskId = event.taskId,
                message = event.message,
                output = event.output,
                success = event.success,
                timestamp = event.timestamp.toString()
            )
        }
    }
}
