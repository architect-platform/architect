package io.github.architectplatform.engine.cloud

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

/**
 * HTTP client interface for communicating with the Architect Cloud Backend.
 */
@Client("\${architect.cloud.url:http://localhost:8080}")
interface CloudClient {
    
    @Post("/api/engines")
    fun registerEngine(@Body request: RegisterEngineRequest)
    
    @Post("/api/engines/heartbeat")
    fun heartbeat(@Body request: HeartbeatRequest)
    
    @Post("/api/projects")
    fun registerProject(@Body request: RegisterProjectRequest)
    
    @Post("/api/executions")
    fun reportExecution(@Body request: ReportExecutionRequest)
    
    @Post("/api/executions/events")
    fun reportEvent(@Body request: ReportEventRequest)
}
