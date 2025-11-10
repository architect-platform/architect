package io.github.architectplatform.server.adapters.inbound.rest.dto

import io.github.architectplatform.server.application.domain.AppliedResource
import io.github.architectplatform.server.application.domain.DeploymentCommand
import io.github.architectplatform.server.application.domain.DeploymentResult
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class DeploymentCommandResponse(
    val id: String,
    val agentId: String,
    val resourceDefinitionId: String,
    val resourceName: String,
    val namespace: String,
    val operation: String,
    val status: String,
    val createdAt: String,
    val sentAt: String?,
    val completedAt: String?,
    val result: DeploymentResultDTO?
) {
    companion object {
        fun fromDomain(command: DeploymentCommand) = DeploymentCommandResponse(
            id = command.id,
            agentId = command.agentId,
            resourceDefinitionId = command.resourceDefinitionId,
            resourceName = command.resourceName,
            namespace = command.namespace,
            operation = command.operation.name,
            status = command.status.name,
            createdAt = command.createdAt.toString(),
            sentAt = command.sentAt?.toString(),
            completedAt = command.completedAt?.toString(),
            result = command.result?.let { DeploymentResultDTO.fromDomain(it) }
        )
    }
}

@Serdeable
data class DeploymentResultDTO(
    val success: Boolean,
    val message: String?,
    val appliedResources: List<AppliedResourceDTO>,
    val error: String?
) {
    fun toDomain() = DeploymentResult(
        success = success,
        message = message,
        appliedResources = appliedResources.map { it.toDomain() },
        error = error
    )
    
    companion object {
        fun fromDomain(result: DeploymentResult) = DeploymentResultDTO(
            success = result.success,
            message = result.message,
            appliedResources = result.appliedResources.map { AppliedResourceDTO.fromDomain(it) },
            error = result.error
        )
    }
}

@Serdeable
data class AppliedResourceDTO(
    val kind: String,
    val name: String,
    val namespace: String,
    val apiVersion: String
) {
    fun toDomain() = AppliedResource(
        kind = kind,
        name = name,
        namespace = namespace,
        apiVersion = apiVersion
    )
    
    companion object {
        fun fromDomain(resource: AppliedResource) = AppliedResourceDTO(
            kind = resource.kind,
            name = resource.name,
            namespace = resource.namespace,
            apiVersion = resource.apiVersion
        )
    }
}

@Serdeable
data class ReportDeploymentResultRequest(
    val commandId: String,
    val result: DeploymentResultDTO
)
