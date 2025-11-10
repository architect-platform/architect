package io.github.architectplatform.server.adapters.inbound.rest.dto

import io.github.architectplatform.server.application.domain.*
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateResourceDefinitionRequest(
    val name: String,
    val version: String,
    val namespace: String = "default",
    val image: String? = null,
    val replicas: Int = 1,
    val environmentVariables: Map<String, String> = emptyMap(),
    val ports: List<PortDTO> = emptyList(),
    val resources: ResourceRequirementsDTO? = null,
    val labels: Map<String, String> = emptyMap(),
    val annotations: Map<String, String> = emptyMap(),
    val templateIds: List<String> = emptyList()
)

@Serdeable
data class UpdateResourceDefinitionRequest(
    val version: String? = null,
    val image: String? = null,
    val replicas: Int? = null,
    val environmentVariables: Map<String, String>? = null,
    val ports: List<PortDTO>? = null,
    val resources: ResourceRequirementsDTO? = null,
    val labels: Map<String, String>? = null,
    val annotations: Map<String, String>? = null,
    val templateIds: List<String>? = null
)

@Serdeable
data class PortDTO(
    val name: String,
    val containerPort: Int,
    val protocol: String = "TCP",
    val servicePort: Int? = null
) {
    fun toDomain() = Port(
        name = name,
        containerPort = containerPort,
        protocol = protocol,
        servicePort = servicePort
    )
    
    companion object {
        fun fromDomain(port: Port) = PortDTO(
            name = port.name,
            containerPort = port.containerPort,
            protocol = port.protocol,
            servicePort = port.servicePort
        )
    }
}

@Serdeable
data class ResourceRequirementsDTO(
    val requests: ResourceSpecDTO? = null,
    val limits: ResourceSpecDTO? = null
) {
    fun toDomain() = ResourceRequirements(
        requests = requests?.toDomain(),
        limits = limits?.toDomain()
    )
    
    companion object {
        fun fromDomain(req: ResourceRequirements) = ResourceRequirementsDTO(
            requests = req.requests?.let { ResourceSpecDTO.fromDomain(it) },
            limits = req.limits?.let { ResourceSpecDTO.fromDomain(it) }
        )
    }
}

@Serdeable
data class ResourceSpecDTO(
    val cpu: String? = null,
    val memory: String? = null
) {
    fun toDomain() = ResourceSpec(cpu = cpu, memory = memory)
    
    companion object {
        fun fromDomain(spec: ResourceSpec) = ResourceSpecDTO(
            cpu = spec.cpu,
            memory = spec.memory
        )
    }
}

@Serdeable
data class ResourceDefinitionResponse(
    val id: String,
    val name: String,
    val version: String,
    val namespace: String,
    val image: String?,
    val replicas: Int,
    val environmentVariables: Map<String, String>,
    val ports: List<PortDTO>,
    val resources: ResourceRequirementsDTO?,
    val labels: Map<String, String>,
    val annotations: Map<String, String>,
    val templateIds: List<String>,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromDomain(resource: ResourceDefinition) = ResourceDefinitionResponse(
            id = resource.id,
            name = resource.name,
            version = resource.version,
            namespace = resource.namespace,
            image = resource.image,
            replicas = resource.replicas,
            environmentVariables = resource.environmentVariables,
            ports = resource.ports.map { PortDTO.fromDomain(it) },
            resources = resource.resources?.let { ResourceRequirementsDTO.fromDomain(it) },
            labels = resource.labels,
            annotations = resource.annotations,
            templateIds = resource.templateIds,
            createdAt = resource.createdAt.toString(),
            updatedAt = resource.updatedAt.toString()
        )
    }
}
