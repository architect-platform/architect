package io.github.architectplatform.server.application.ports.outbound

import io.github.architectplatform.server.application.domain.ApplicationDefinition

/**
 * Outbound port for application definition persistence.
 * Clean interface following Repository pattern.
 */
interface ApplicationDefinitionPort {
    
    fun save(applicationDefinition: ApplicationDefinition): ApplicationDefinition
    
    fun findById(id: String): ApplicationDefinition?
    
    fun findAll(): List<ApplicationDefinition>
    
    fun deleteById(id: String): Boolean
    
    fun existsById(id: String): Boolean
}
