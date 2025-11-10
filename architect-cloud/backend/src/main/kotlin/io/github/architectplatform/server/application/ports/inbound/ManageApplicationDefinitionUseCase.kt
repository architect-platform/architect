package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.*

/**
 * Use case interface for managing platform-agnostic application definitions.
 * Clean, simple API with support for dependencies (databases, brokers, etc.).
 */
interface ManageApplicationDefinitionUseCase {
    
    fun createApplicationDefinition(
        name: String,
        version: String,
        type: ApplicationType,
        image: String,
        instances: Int,
        environment: Map<String, String>,
        exposedPorts: List<ExposedPort>,
        resources: ResourceLimits?,
        healthCheck: HealthCheck?,
        metadata: Map<String, String>,
        dependencies: List<Dependency>,
        templateIds: List<String>
    ): ApplicationDefinition
    
    fun updateApplicationDefinition(
        appId: String,
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
        templateIds: List<String>? = null
    ): ApplicationDefinition?
    
    fun getApplicationDefinition(appId: String): ApplicationDefinition?
    
    fun getAllApplicationDefinitions(): List<ApplicationDefinition>
    
    fun deleteApplicationDefinition(appId: String): Boolean
}
