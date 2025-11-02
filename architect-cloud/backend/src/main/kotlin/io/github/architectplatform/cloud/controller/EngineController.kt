package io.github.architectplatform.cloud.controller

import io.github.architectplatform.cloud.domain.EngineInstance
import io.github.architectplatform.cloud.dto.HeartbeatRequest
import io.github.architectplatform.cloud.dto.RegisterEngineRequest
import io.github.architectplatform.cloud.service.CloudService
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/api/engines")
@ExecuteOn(TaskExecutors.IO)
class EngineController(private val cloudService: CloudService) {
    
    @Post
    fun registerEngine(@Body request: RegisterEngineRequest): EngineInstance {
        return cloudService.registerEngine(request)
    }
    
    @Post("/heartbeat")
    fun heartbeat(@Body request: HeartbeatRequest) {
        cloudService.heartbeat(request.engineId)
    }
    
    @Get("/{engineId}")
    fun getEngine(@PathVariable engineId: String): EngineInstance? {
        return cloudService.getEngine(engineId)
    }
    
    @Get
    fun getAllEngines(): List<EngineInstance> {
        return cloudService.getAllEngines()
    }
    
    @Get("/active")
    fun getActiveEngines(): List<EngineInstance> {
        return cloudService.getActiveEngines()
    }
}
