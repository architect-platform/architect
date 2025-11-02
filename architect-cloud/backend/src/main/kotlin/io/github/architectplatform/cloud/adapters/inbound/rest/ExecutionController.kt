package io.github.architectplatform.cloud.adapters.inbound.rest

import io.github.architectplatform.cloud.adapters.inbound.rest.dto.ExecutionEventResponse
import io.github.architectplatform.cloud.adapters.inbound.rest.dto.ExecutionResponse
import io.github.architectplatform.cloud.adapters.inbound.rest.dto.ReportEventRequest
import io.github.architectplatform.cloud.adapters.inbound.rest.dto.ReportExecutionRequest
import io.github.architectplatform.cloud.application.domain.ExecutionStatus
import io.github.architectplatform.cloud.application.ports.inbound.TrackExecutionEventUseCase
import io.github.architectplatform.cloud.application.ports.inbound.TrackExecutionUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * REST controller for execution tracking.
 * Inbound adapter that exposes execution use cases via HTTP.
 */
@Controller("/api/executions")
@ExecuteOn(TaskExecutors.IO)
class ExecutionController(
    private val trackExecutionUseCase: TrackExecutionUseCase,
    private val trackExecutionEventUseCase: TrackExecutionEventUseCase
) {
    
    @Post
    fun reportExecution(@Body request: ReportExecutionRequest): ExecutionResponse {
        val execution = trackExecutionUseCase.reportExecution(
            id = request.id,
            projectId = request.projectId,
            engineId = request.engineId,
            taskId = request.taskId,
            status = ExecutionStatus.valueOf(request.status),
            message = request.message,
            errorDetails = request.errorDetails
        )
        return ExecutionResponse.fromDomain(execution)
    }
    
    @Get("/{executionId}")
    fun getExecution(@PathVariable executionId: String): ExecutionResponse? {
        return trackExecutionUseCase.getExecution(executionId)
            ?.let { ExecutionResponse.fromDomain(it) }
    }
    
    @Get("/project/{projectId}")
    fun getExecutionsByProject(@PathVariable projectId: String): List<ExecutionResponse> {
        return trackExecutionUseCase.getExecutionsByProject(projectId)
            .map { ExecutionResponse.fromDomain(it) }
    }
    
    @Get("/engine/{engineId}")
    fun getExecutionsByEngine(@PathVariable engineId: String): List<ExecutionResponse> {
        return trackExecutionUseCase.getExecutionsByEngine(engineId)
            .map { ExecutionResponse.fromDomain(it) }
    }
    
    @Post("/events")
    fun reportEvent(@Body request: ReportEventRequest): ExecutionEventResponse {
        val event = trackExecutionEventUseCase.reportEvent(
            id = request.id,
            executionId = request.executionId,
            eventType = request.eventType,
            taskId = request.taskId,
            message = request.message,
            output = request.output,
            success = request.success
        )
        return ExecutionEventResponse.fromDomain(event)
    }
    
    @Get("/{executionId}/events")
    fun getExecutionEvents(@PathVariable executionId: String): List<ExecutionEventResponse> {
        return trackExecutionEventUseCase.getExecutionEvents(executionId)
            .map { ExecutionEventResponse.fromDomain(it) }
    }
}
