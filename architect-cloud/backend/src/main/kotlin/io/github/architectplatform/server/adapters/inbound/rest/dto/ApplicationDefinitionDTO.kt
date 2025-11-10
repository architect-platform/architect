package io.github.architectplatform.server.adapters.inbound.rest.dto

import io.github.architectplatform.server.application.domain.*
import io.micronaut.serde.annotation.Serdeable

/**
 * Platform-agnostic application definition request.
 * Clean, simple API for creating deployable applications with dependencies.
 */
@Serdeable
data class CreateApplicationDefinitionRequest(
    val name: String,
    val version: String,
    val type: ApplicationType = ApplicationType.APPLICATION,
    val image: String,
    val instances: Int = 1,
    val environment: Map<String, String> = emptyMap(),
    val exposedPorts: List<ExposedPortDTO> = emptyList(),
    val resources: ResourceLimitsDTO? = null,
    val healthCheck: HealthCheckDTO? = null,
    val metadata: Map<String, String> = emptyMap(),
    val dependencies: List<DependencyDTO> = emptyList(),
    val templateIds: List<String> = emptyList()
)

@Serdeable
data class UpdateApplicationDefinitionRequest(
    val version: String? = null,
    val type: ApplicationType? = null,
    val image: String? = null,
    val instances: Int? = null,
    val environment: Map<String, String>? = null,
    val exposedPorts: List<ExposedPortDTO>? = null,
    val resources: ResourceLimitsDTO? = null,
    val healthCheck: HealthCheckDTO? = null,
    val metadata: Map<String, String>? = null,
    val dependencies: List<DependencyDTO>? = null,
    val templateIds: List<String>? = null
)

@Serdeable
data class ExposedPortDTO(
    val port: Int,
    val protocol: String = "TCP",
    val public: Boolean = false
) {
    fun toDomain() = ExposedPort(
        port = port,
        protocol = protocol,
        public = public
    )
    
    companion object {
        fun fromDomain(port: ExposedPort) = ExposedPortDTO(
            port = port.port,
            protocol = port.protocol,
            public = port.public
        )
    }
}

@Serdeable
data class ResourceLimitsDTO(
    val cpu: String? = null,
    val memory: String? = null,
    val storage: String? = null
) {
    fun toDomain() = ResourceLimits(
        cpu = cpu,
        memory = memory,
        storage = storage
    )
    
    companion object {
        fun fromDomain(res: ResourceLimits) = ResourceLimitsDTO(
            cpu = res.cpu,
            memory = res.memory,
            storage = res.storage
        )
    }
}

@Serdeable
data class HealthCheckDTO(
    val type: HealthCheckType,
    val path: String? = null,
    val port: Int? = null,
    val intervalSeconds: Int = 30,
    val timeoutSeconds: Int = 5
) {
    fun toDomain() = HealthCheck(
        type = type,
        path = path,
        port = port,
        intervalSeconds = intervalSeconds,
        timeoutSeconds = timeoutSeconds
    )
    
    companion object {
        fun fromDomain(hc: HealthCheck) = HealthCheckDTO(
            type = hc.type,
            path = hc.path,
            port = hc.port,
            intervalSeconds = hc.intervalSeconds,
            timeoutSeconds = hc.timeoutSeconds
        )
    }
}

@Serdeable
data class DependencyDTO(
    val applicationId: String,
    val required: Boolean = true,
    val connectionInfo: Map<String, String> = emptyMap()
) {
    fun toDomain() = Dependency(
        applicationId = applicationId,
        required = required,
        connectionInfo = connectionInfo
    )
    
    companion object {
        fun fromDomain(dep: Dependency) = DependencyDTO(
            applicationId = dep.applicationId,
            required = dep.required,
            connectionInfo = dep.connectionInfo
        )
    }
}

@Serdeable
data class ApplicationDefinitionResponse(
    val id: String,
    val name: String,
    val version: String,
    val type: String,
    val image: String,
    val instances: Int,
    val environment: Map<String, String>,
    val exposedPorts: List<ExposedPortDTO>,
    val resources: ResourceLimitsDTO?,
    val healthCheck: HealthCheckDTO?,
    val metadata: Map<String, String>,
    val dependencies: List<DependencyDTO>,
    val templateIds: List<String>,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromDomain(app: ApplicationDefinition) = ApplicationDefinitionResponse(
            id = app.id,
            name = app.name,
            version = app.version,
            type = app.type.name,
            image = app.image,
            instances = app.instances,
            environment = app.environment,
            exposedPorts = app.exposedPorts.map { ExposedPortDTO.fromDomain(it) },
            resources = app.resources?.let { ResourceLimitsDTO.fromDomain(it) },
            healthCheck = app.healthCheck?.let { HealthCheckDTO.fromDomain(it) },
            metadata = app.metadata,
            dependencies = app.dependencies.map { DependencyDTO.fromDomain(it) },
            templateIds = app.templateIds,
            createdAt = app.createdAt.toString(),
            updatedAt = app.updatedAt.toString()
        )
    }
}
