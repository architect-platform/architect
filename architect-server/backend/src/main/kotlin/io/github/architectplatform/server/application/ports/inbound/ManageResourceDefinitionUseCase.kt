package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.Port
import io.github.architectplatform.server.application.domain.ResourceDefinition
import io.github.architectplatform.server.application.domain.ResourceRequirements

/**
 * Use case interface for managing resource definitions.
 * Inbound port defining what the application can do with resource definitions.
 */
interface ManageResourceDefinitionUseCase {
    
    /**
     * Create a new resource definition
     */
    fun createResourceDefinition(
        name: String,
        version: String,
        namespace: String,
        image: String?,
        replicas: Int,
        environmentVariables: Map<String, String>,
        ports: List<Port>,
        resources: ResourceRequirements?,
        labels: Map<String, String>,
        annotations: Map<String, String>,
        templateIds: List<String>
    ): ResourceDefinition
    
    /**
     * Update an existing resource definition
     */
    fun updateResourceDefinition(
        resourceId: String,
        version: String? = null,
        image: String? = null,
        replicas: Int? = null,
        environmentVariables: Map<String, String>? = null,
        ports: List<Port>? = null,
        resources: ResourceRequirements? = null,
        labels: Map<String, String>? = null,
        annotations: Map<String, String>? = null,
        templateIds: List<String>? = null
    ): ResourceDefinition?
    
    /**
     * Get a resource definition by ID
     */
    fun getResourceDefinition(resourceId: String): ResourceDefinition?
    
    /**
     * Get all resource definitions
     */
    fun getAllResourceDefinitions(): List<ResourceDefinition>
    
    /**
     * Get resource definitions by namespace
     */
    fun getResourceDefinitionsByNamespace(namespace: String): List<ResourceDefinition>
    
    /**
     * Delete a resource definition
     */
    fun deleteResourceDefinition(resourceId: String): Boolean
}
