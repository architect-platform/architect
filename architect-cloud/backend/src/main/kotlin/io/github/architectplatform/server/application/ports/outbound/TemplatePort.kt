package io.github.architectplatform.server.application.ports.outbound

import io.github.architectplatform.server.application.domain.Template
import io.github.architectplatform.server.application.domain.TemplateType

/**
 * Outbound port for template persistence.
 * Defines what the application needs from template storage.
 */
interface TemplatePort {
    
    fun save(template: Template): Template
    
    fun findById(id: String): Template?
    
    fun findAll(): List<Template>
    
    fun findByType(templateType: TemplateType): List<Template>
    
    fun deleteById(id: String): Boolean
    
    fun existsById(id: String): Boolean
}
