package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Platform-agnostic application definition domain model.
 * Clean, simple representation of a deployable application.
 * Supports dependencies on databases, brokers, and other services.
 * Pure business logic, no platform-specific concepts.
 * 
 * NEW: Enhanced with environment management, tagging, and multi-cloud support
 */
data class ApplicationDefinition(
    val id: String,
    val name: String,
    val version: String,
    val type: ApplicationType = ApplicationType.APPLICATION,
    val image: String,
    val instances: Int = 1,
    val environment: Map<String, String> = emptyMap(),
    val exposedPorts: List<ExposedPort> = emptyList(),
    val resources: ResourceLimits? = null,
    val healthCheck: HealthCheck? = null,
    val metadata: Map<String, String> = emptyMap(),
    val dependencies: List<Dependency> = emptyList(),
    val templateIds: List<String> = emptyList(),
    val targetEnvironment: DeploymentEnvironment = DeploymentEnvironment.DEVELOPMENT,
    val tags: Map<String, String> = emptyMap(),
    val cloudProvider: CloudProvider? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun update(
        version: String? = null,
        type: ApplicationType? = null,
        image: String? = null,
        instances: Int? = null,
        environment: Map<String, String>? = null,
        exposedPorts: List<ExposedPort>? = null,
        resources: ResourceLimits? = null,
        healthCheck: HealthCheck? = null,
        metadata: Map<String, String>? = null,
        dependencies: List<Dependency>? = null,
        templateIds: List<String>? = null,
        targetEnvironment: DeploymentEnvironment? = null,
        tags: Map<String, String>? = null,
        cloudProvider: CloudProvider? = null
    ): ApplicationDefinition {
        return copy(
            version = version ?: this.version,
            type = type ?: this.type,
            image = image ?: this.image,
            instances = instances ?: this.instances,
            environment = environment ?: this.environment,
            exposedPorts = exposedPorts ?: this.exposedPorts,
            resources = resources ?: this.resources,
            healthCheck = healthCheck ?: this.healthCheck,
            metadata = metadata ?: this.metadata,
            dependencies = dependencies ?: this.dependencies,
            templateIds = templateIds ?: this.templateIds,
            targetEnvironment = targetEnvironment ?: this.targetEnvironment,
            tags = tags ?: this.tags,
            cloudProvider = cloudProvider ?: this.cloudProvider,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Apply environment-specific overrides
     */
    fun withEnvironmentOverrides(envOverrides: Map<String, Any>): ApplicationDefinition {
        val newEnvironment = environment.toMutableMap()
        envOverrides.forEach { (key, value) ->
            if (value is String) {
                newEnvironment[key] = value
            }
        }
        return copy(environment = newEnvironment, updatedAt = Instant.now())
    }
    
    /**
     * Add or update tags
     */
    fun withTags(newTags: Map<String, String>): ApplicationDefinition {
        return copy(tags = tags + newTags, updatedAt = Instant.now())
    }
    
    /**
     * Get all dependency IDs in deployment order.
     * Required dependencies come first.
     */
    fun getDependenciesInOrder(): List<String> {
        return dependencies
            .sortedByDescending { it.required }
            .map { it.applicationId }
    }

    /**
     * Convert application definition to variables for template rendering.
     * This provides a clean, platform-agnostic view that agents can map
     * to their specific platform implementations.
     */
    fun toVariableMap(): Map<String, Any> {
        val vars = mutableMapOf<String, Any>(
            "name" to name,
            "version" to version,
            "type" to type.name,
            "image" to image,
            "instances" to instances,
            "environment" to environment,
            "metadata" to metadata
        )

        if (exposedPorts.isNotEmpty()) {
            vars["ports"] = exposedPorts.map {
                mapOf(
                    "port" to it.port,
                    "protocol" to it.protocol,
                    "public" to it.public
                )
            }
        }
        
        // Add environment and cloud provider info
        vars["targetEnvironment"] = targetEnvironment.name.lowercase()
        cloudProvider?.let { vars["cloudProvider"] = it.name.lowercase() }
        
        if (tags.isNotEmpty()) {
            vars["tags"] = tags
        }

        resources?.let { res ->
            val resourcesMap = mutableMapOf<String, String>()
            res.cpu?.let { resourcesMap["cpu"] = it }
            res.memory?.let { resourcesMap["memory"] = it }
            res.storage?.let { resourcesMap["storage"] = it }
            if (resourcesMap.isNotEmpty()) {
                vars["resources"] = resourcesMap
            }
        }

        healthCheck?.let { hc ->
            val healthCheckMap = mutableMapOf<String, Any>(
                "type" to hc.type.name,
                "intervalSeconds" to hc.intervalSeconds,
                "timeoutSeconds" to hc.timeoutSeconds
            )
            hc.path?.let { healthCheckMap["path"] = it }
            hc.port?.let { healthCheckMap["port"] = it }
            vars["healthCheck"] = healthCheckMap
        }

        return vars
    }
}

/**
 * Simple port exposure (platform-agnostic)
 */
data class ExposedPort(
    val port: Int,
    val protocol: String = "TCP",
    val public: Boolean = false
)

/**
 * Resource limits (platform-agnostic)
 */
data class ResourceLimits(
    val cpu: String? = null,
    val memory: String? = null,
    val storage: String? = null
)

/**
 * Health check configuration
 */
data class HealthCheck(
    val type: HealthCheckType,
    val path: String? = null,
    val port: Int? = null,
    val intervalSeconds: Int = 30,
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
    APPLICATION,    // Regular application
    DATABASE,       // Database service (PostgreSQL, MySQL, MongoDB, etc.)
    MESSAGE_BROKER, // Message broker (RabbitMQ, Kafka, Redis, etc.)
    CACHE,          // Cache service (Redis, Memcached, etc.)
    STORAGE,        // Storage service (MinIO, S3-compatible, etc.)
    SERVICE         // Generic service
}

/**
 * Dependency on another application.
 * Clean, simple dependency management.
 */
data class Dependency(
    val applicationId: String,
    val required: Boolean = true,
    val connectionInfo: Map<String, String> = emptyMap()
) {
    /**
     * Check if this is a required dependency that must be deployed first.
     */
    fun isRequired(): Boolean = required
}

/**
 * Deployment environment types
 */
enum class DeploymentEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
    TEST
}

/**
 * Cloud provider types for multi-cloud support
 */
enum class CloudProvider {
    AWS,
    GCP,
    AZURE,
    KUBERNETES,
    DOCKER,
    ON_PREMISE
}
