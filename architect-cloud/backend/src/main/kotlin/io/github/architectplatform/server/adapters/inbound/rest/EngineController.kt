package io.github.architectplatform.server.adapters.inbound.rest

import io.github.architectplatform.server.adapters.inbound.rest.dto.EngineInstanceResponse
import io.github.architectplatform.server.adapters.inbound.rest.dto.HeartbeatRequest
import io.github.architectplatform.server.adapters.inbound.rest.dto.RegisterEngineRequest
import io.github.architectplatform.server.application.ports.inbound.ManageEngineUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * REST controller for engine management.
 * Inbound adapter that exposes engine use cases via HTTP.
 */
@Controller("/api/engines")
@ExecuteOn(TaskExecutors.IO)
class EngineController(
    private val manageEngineUseCase: ManageEngineUseCase
) {
    
    @Post
    fun registerEngine(@Body request: RegisterEngineRequest): EngineInstanceResponse {
        val engine = manageEngineUseCase.registerEngine(
            id = request.id,
            hostname = request.hostname,
            port = request.port,
            version = request.version
        )
        return EngineInstanceResponse.fromDomain(engine)
    }
    
    @Post("/heartbeat")
    fun heartbeat(@Body request: HeartbeatRequest) {
        manageEngineUseCase.recordHeartbeat(request.engineId)
    }
    
    @Get("/{engineId}")
    fun getEngine(@PathVariable engineId: String): EngineInstanceResponse? {
        return manageEngineUseCase.getEngine(engineId)
            ?.let { EngineInstanceResponse.fromDomain(it) }
    }
    
    @Get
    fun getAllEngines(): List<EngineInstanceResponse> {
        return manageEngineUseCase.getAllEngines()
            .map { EngineInstanceResponse.fromDomain(it) }
    }
    
    @Get("/active")
    fun getActiveEngines(): List<EngineInstanceResponse> {
        return manageEngineUseCase.getActiveEngines()
            .map { EngineInstanceResponse.fromDomain(it) }
    }
}
