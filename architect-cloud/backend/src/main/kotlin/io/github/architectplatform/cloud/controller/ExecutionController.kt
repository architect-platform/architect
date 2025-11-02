package io.github.architectplatform.cloud.controller

import io.github.architectplatform.cloud.domain.Execution
import io.github.architectplatform.cloud.domain.ExecutionEvent
import io.github.architectplatform.cloud.dto.ReportEventRequest
import io.github.architectplatform.cloud.dto.ReportExecutionRequest
import io.github.architectplatform.cloud.service.CloudService
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/api/executions")
@ExecuteOn(TaskExecutors.IO)
class ExecutionController(private val cloudService: CloudService) {
    
    @Post
    fun reportExecution(@Body request: ReportExecutionRequest): Execution {
        return cloudService.reportExecution(request)
    }
    
    @Get("/{executionId}")
    fun getExecution(@PathVariable executionId: String): Execution? {
        return cloudService.getExecution(executionId)
    }
    
    @Get("/project/{projectId}")
    fun getExecutionsByProject(@PathVariable projectId: String): List<Execution> {
        return cloudService.getExecutionsByProject(projectId)
    }
    
    @Get("/engine/{engineId}")
    fun getExecutionsByEngine(@PathVariable engineId: String): List<Execution> {
        return cloudService.getExecutionsByEngine(engineId)
    }
    
    @Post("/events")
    fun reportEvent(@Body request: ReportEventRequest): ExecutionEvent {
        return cloudService.reportEvent(request)
    }
    
    @Get("/{executionId}/events")
    fun getExecutionEvents(@PathVariable executionId: String): List<ExecutionEvent> {
        return cloudService.getExecutionEvents(executionId)
    }
}
