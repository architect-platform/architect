package io.github.architectplatform.server.adapters.inbound.rest

import io.github.architectplatform.server.adapters.inbound.rest.dto.*
import io.github.architectplatform.server.application.domain.DeploymentOperation
import io.github.architectplatform.server.application.ports.inbound.ManageDeploymentCommandUseCase
import io.github.architectplatform.server.application.ports.inbound.ManageResourceDefinitionUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * REST controller for resource definition management.
 * Applies facade pattern to hide complexity of resource operations.
 */
@Controller("/api/resources")
@ExecuteOn(TaskExecutors.IO)
class ResourceDefinitionController(
    private val manageResourceDefinitionUseCase: ManageResourceDefinitionUseCase,
    private val manageDeploymentCommandUseCase: ManageDeploymentCommandUseCase
) {

    @Post
    fun createResourceDefinition(@Body request: CreateResourceDefinitionRequest): ResourceDefinitionResponse {
        val resource = manageResourceDefinitionUseCase.createResourceDefinition(
            name = request.name,
            version = request.version,
            namespace = request.namespace,
            image = request.image,
            replicas = request.replicas,
            environmentVariables = request.environmentVariables,
            ports = request.ports.map { it.toDomain() },
            resources = request.resources?.toDomain(),
            labels = request.labels,
            annotations = request.annotations,
            templateIds = request.templateIds
        )
        return ResourceDefinitionResponse.fromDomain(resource)
    }

    @Put("/{resourceId}")
    fun updateResourceDefinition(
        @PathVariable resourceId: String,
        @Body request: UpdateResourceDefinitionRequest
    ): ResourceDefinitionResponse? {
        val resource = manageResourceDefinitionUseCase.updateResourceDefinition(
            resourceId = resourceId,
            version = request.version,
            image = request.image,
            replicas = request.replicas,
            environmentVariables = request.environmentVariables,
            ports = request.ports?.map { it.toDomain() },
            resources = request.resources?.toDomain(),
            labels = request.labels,
            annotations = request.annotations,
            templateIds = request.templateIds
        )
        return resource?.let { ResourceDefinitionResponse.fromDomain(it) }
    }

    @Get("/{resourceId}")
    fun getResourceDefinition(@PathVariable resourceId: String): ResourceDefinitionResponse? {
        return manageResourceDefinitionUseCase.getResourceDefinition(resourceId)
            ?.let { ResourceDefinitionResponse.fromDomain(it) }
    }

    @Get
    fun getAllResourceDefinitions(): List<ResourceDefinitionResponse> {
        return manageResourceDefinitionUseCase.getAllResourceDefinitions()
            .map { ResourceDefinitionResponse.fromDomain(it) }
    }

    @Get("/namespace/{namespace}")
    fun getResourceDefinitionsByNamespace(@PathVariable namespace: String): List<ResourceDefinitionResponse> {
        return manageResourceDefinitionUseCase.getResourceDefinitionsByNamespace(namespace)
            .map { ResourceDefinitionResponse.fromDomain(it) }
    }

    @Delete("/{resourceId}")
    fun deleteResourceDefinition(@PathVariable resourceId: String): Map<String, Boolean> {
        val deleted = manageResourceDefinitionUseCase.deleteResourceDefinition(resourceId)
        return mapOf("deleted" to deleted)
    }

    @Post("/{resourceId}/deploy")
    fun deployResource(
        @PathVariable resourceId: String,
        @QueryValue agentId: String,
        @QueryValue(defaultValue = "APPLY") operation: DeploymentOperation
    ): DeploymentCommandResponse {
        val command = manageDeploymentCommandUseCase.createDeploymentCommand(
            resourceDefinitionId = resourceId,
            agentId = agentId,
            operation = operation
        )
        return DeploymentCommandResponse.fromDomain(command)
    }
}
