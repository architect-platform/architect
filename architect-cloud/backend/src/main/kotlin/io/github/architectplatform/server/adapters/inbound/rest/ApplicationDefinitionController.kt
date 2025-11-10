package io.github.architectplatform.server.adapters.inbound.rest

import io.github.architectplatform.server.adapters.inbound.rest.dto.*
import io.github.architectplatform.server.application.domain.DeploymentOperation
import io.github.architectplatform.server.application.ports.inbound.ManageDeploymentCommandUseCase
import io.github.architectplatform.server.application.ports.inbound.ManageApplicationDefinitionUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * Clean REST API for managing platform-agnostic application definitions.
 * Simple, focused controller following REST best practices.
 */
@Controller("/api/applications")
@ExecuteOn(TaskExecutors.IO)
class ApplicationDefinitionController(
    private val manageApplicationDefinitionUseCase: ManageApplicationDefinitionUseCase,
    private val manageDeploymentCommandUseCase: ManageDeploymentCommandUseCase
) {

    @Post
    fun createApplication(@Body request: CreateApplicationDefinitionRequest): ApplicationDefinitionResponse {
        val app = manageApplicationDefinitionUseCase.createApplicationDefinition(
            name = request.name,
            version = request.version,
            type = request.type,
            image = request.image,
            instances = request.instances,
            environment = request.environment,
            exposedPorts = request.exposedPorts.map { it.toDomain() },
            resources = request.resources?.toDomain(),
            healthCheck = request.healthCheck?.toDomain(),
            metadata = request.metadata,
            dependencies = request.dependencies.map { it.toDomain() },
            templateIds = request.templateIds
        )
        return ApplicationDefinitionResponse.fromDomain(app)
    }

    @Put("/{appId}")
    fun updateApplication(
        @PathVariable appId: String,
        @Body request: UpdateApplicationDefinitionRequest
    ): ApplicationDefinitionResponse? {
        val app = manageApplicationDefinitionUseCase.updateApplicationDefinition(
            appId = appId,
            version = request.version,
            type = request.type,
            image = request.image,
            instances = request.instances,
            environment = request.environment,
            exposedPorts = request.exposedPorts?.map { it.toDomain() },
            resources = request.resources?.toDomain(),
            healthCheck = request.healthCheck?.toDomain(),
            metadata = request.metadata,
            dependencies = request.dependencies?.map { it.toDomain() },
            templateIds = request.templateIds
        )
        return app?.let { ApplicationDefinitionResponse.fromDomain(it) }
    }

    @Get("/{appId}")
    fun getApplication(@PathVariable appId: String): ApplicationDefinitionResponse? {
        return manageApplicationDefinitionUseCase.getApplicationDefinition(appId)
            ?.let { ApplicationDefinitionResponse.fromDomain(it) }
    }

    @Get
    fun getAllApplications(): List<ApplicationDefinitionResponse> {
        return manageApplicationDefinitionUseCase.getAllApplicationDefinitions()
            .map { ApplicationDefinitionResponse.fromDomain(it) }
    }

    @Delete("/{appId}")
    fun deleteApplication(@PathVariable appId: String): Map<String, Boolean> {
        val deleted = manageApplicationDefinitionUseCase.deleteApplicationDefinition(appId)
        return mapOf("deleted" to deleted)
    }

    @Post("/{appId}/deploy")
    fun deployApplication(
        @PathVariable appId: String,
        @QueryValue agentId: String,
        @QueryValue(defaultValue = "APPLY") operation: DeploymentOperation
    ): DeploymentCommandResponse {
        val command = manageDeploymentCommandUseCase.createDeploymentCommand(
            resourceDefinitionId = appId,
            agentId = agentId,
            operation = operation
        )
        return DeploymentCommandResponse.fromDomain(command)
    }
}
