package io.github.architectplatform.server.adapters.outbound.inmemory

import io.github.architectplatform.server.application.domain.ResourceDefinition
import io.github.architectplatform.server.application.ports.outbound.ResourceDefinitionPort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of ResourceDefinitionPort for development and testing.
 * Follows repository pattern with thread-safe operations.
 */
@Singleton
class InMemoryResourceDefinitionAdapter : ResourceDefinitionPort {
    
    private val resources = ConcurrentHashMap<String, ResourceDefinition>()
    
    override fun save(resourceDefinition: ResourceDefinition): ResourceDefinition {
        resources[resourceDefinition.id] = resourceDefinition
        return resourceDefinition
    }
    
    override fun findById(id: String): ResourceDefinition? = resources[id]
    
    override fun findAll(): List<ResourceDefinition> = resources.values.toList()
    
    override fun findByNamespace(namespace: String): List<ResourceDefinition> {
        return resources.values.filter { it.namespace == namespace }
    }
    
    override fun deleteById(id: String): Boolean = resources.remove(id) != null
    
    override fun existsById(id: String): Boolean = resources.containsKey(id)
}
