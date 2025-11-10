package io.github.architectplatform.server.adapters.outbound.inmemory

import io.github.architectplatform.server.application.domain.ApplicationDefinition
import io.github.architectplatform.server.application.ports.outbound.ApplicationDefinitionPort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation for development and testing.
 * Clean, thread-safe repository implementation.
 */
@Singleton
class InMemoryApplicationDefinitionAdapter : ApplicationDefinitionPort {
    
    private val applications = ConcurrentHashMap<String, ApplicationDefinition>()
    
    override fun save(applicationDefinition: ApplicationDefinition): ApplicationDefinition {
        applications[applicationDefinition.id] = applicationDefinition
        return applicationDefinition
    }
    
    override fun findById(id: String): ApplicationDefinition? = applications[id]
    
    override fun findAll(): List<ApplicationDefinition> = applications.values.toList()
    
    override fun deleteById(id: String): Boolean = applications.remove(id) != null
    
    override fun existsById(id: String): Boolean = applications.containsKey(id)
}
