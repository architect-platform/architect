package io.github.architectplatform.server.application.ports.outbound

import io.github.architectplatform.server.application.domain.ResourceDefinition

/**
 * Outbound port for resource definition persistence.
 * Defines what the application needs from resource definition storage.
 */
interface ResourceDefinitionPort {
    
    fun save(resourceDefinition: ResourceDefinition): ResourceDefinition
    
    fun findById(id: String): ResourceDefinition?
    
    fun findAll(): List<ResourceDefinition>
    
    fun findByNamespace(namespace: String): List<ResourceDefinition>
    
    fun deleteById(id: String): Boolean
    
    fun existsById(id: String): Boolean
}
