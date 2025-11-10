package io.github.architectplatform.server.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Platform-agnostic application definition.
 * Simple, clean API for defining deployable applications.
 * Supports dependencies on databases, brokers, and other services.
 * Agents map these generic concepts to platform-specific implementations.
 */
data class ApplicationDefinitionDTO(
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("version")
    val version: String,
    
    @JsonProperty("type")
    val type: ApplicationType = ApplicationType.APPLICATION,
    
    @JsonProperty("image")
    val image: String,
    
    @JsonProperty("instances")
    val instances: Int = 1,
    
    @JsonProperty("environment")
    val environment: Map<String, String> = emptyMap(),
    
    @JsonProperty("exposedPorts")
    val exposedPorts: List<ExposedPortDTO> = emptyList(),
    
    @JsonProperty("resources")
    val resources: ResourceLimitsDTO? = null,
    
    @JsonProperty("healthCheck")
    val healthCheck: HealthCheckDTO? = null,
    
    @JsonProperty("metadata")
    val metadata: Map<String, String> = emptyMap(),
    
    @JsonProperty("dependencies")
    val dependencies: List<DependencyDTO> = emptyList()
)

/**
 * Simple port exposure definition
 */
data class ExposedPortDTO(
    @JsonProperty("port")
    val port: Int,
    
    @JsonProperty("protocol")
    val protocol: String = "TCP",
    
    @JsonProperty("public")
    val public: Boolean = false
)

/**
 * Resource limits (platform-agnostic)
 */
data class ResourceLimitsDTO(
    @JsonProperty("cpu")
    val cpu: String? = null,
    
    @JsonProperty("memory")
    val memory: String? = null,
    
    @JsonProperty("storage")
    val storage: String? = null
)

/**
 * Health check configuration
 */
data class HealthCheckDTO(
    @JsonProperty("type")
    val type: HealthCheckType,
    
    @JsonProperty("path")
    val path: String? = null,
    
    @JsonProperty("port")
    val port: Int? = null,
    
    @JsonProperty("intervalSeconds")
    val intervalSeconds: Int = 30,
    
    @JsonProperty("timeoutSeconds")
    val timeoutSeconds: Int = 5
)

enum class HealthCheckType {
    HTTP,
    TCP,
    COMMAND
}

/**
 * Application type classification
 */
enum class ApplicationType {
    APPLICATION,
    DATABASE,
    MESSAGE_BROKER,
    CACHE,
    STORAGE,
    SERVICE
}

/**
 * Dependency definition
 */
data class DependencyDTO(
    @JsonProperty("applicationId")
    val applicationId: String,
    
    @JsonProperty("required")
    val required: Boolean = true,
    
    @JsonProperty("connectionInfo")
    val connectionInfo: Map<String, String> = emptyMap()
)
