package io.github.architectplatform.server.adapters.inbound.rest.dto

import io.github.architectplatform.server.application.domain.*
import io.micronaut.serde.annotation.Serdeable

// Request DTOs

@Serdeable
data class RegisterEngineRequest(
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?
)

@Serdeable
data class RegisterProjectRequest(
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String? = null
)

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
data class HeartbeatRequest(
    val engineId: String
)

// Response DTOs

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
        fun fromDomain(domain: EngineInstance): EngineInstanceResponse {
            return EngineInstanceResponse(
                id = domain.id,
                hostname = domain.hostname,
                port = domain.port,
                version = domain.version,
                status = domain.status.name,
                createdAt = domain.createdAt.toString(),
                lastHeartbeat = domain.lastHeartbeat.toString()
            )
        }
    }
}

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
        fun fromDomain(domain: Project): ProjectResponse {
            return ProjectResponse(
                id = domain.id,
                name = domain.name,
                path = domain.path,
                engineId = domain.engineId,
                description = domain.description,
                createdAt = domain.createdAt.toString()
            )
        }
    }
}

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
        fun fromDomain(domain: Execution): ExecutionResponse {
            return ExecutionResponse(
                id = domain.id,
                projectId = domain.projectId,
                engineId = domain.engineId,
                taskId = domain.taskId,
                status = domain.status.name,
                message = domain.message,
                errorDetails = domain.errorDetails,
                startedAt = domain.startedAt.toString(),
                completedAt = domain.completedAt?.toString()
            )
        }
    }
}

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
        fun fromDomain(domain: ExecutionEvent): ExecutionEventResponse {
            return ExecutionEventResponse(
                id = domain.id,
                executionId = domain.executionId,
                eventType = domain.eventType,
                taskId = domain.taskId,
                message = domain.message,
                output = domain.output,
                success = domain.success,
                timestamp = domain.timestamp.toString()
            )
        }
    }
}
