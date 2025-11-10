package io.github.architectplatform.server.adapters.outbound.inmemory

import io.github.architectplatform.server.application.domain.Template
import io.github.architectplatform.server.application.domain.TemplateType
import io.github.architectplatform.server.application.ports.outbound.TemplatePort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of TemplatePort for development and testing.
 */
@Singleton
class InMemoryTemplateAdapter : TemplatePort {
    
    private val templates = ConcurrentHashMap<String, Template>()
    
    override fun save(template: Template): Template {
        templates[template.id] = template
        return template
    }
    
    override fun findById(id: String): Template? {
        return templates[id]
    }
    
    override fun findAll(): List<Template> {
        return templates.values.toList()
    }
    
    override fun findByType(templateType: TemplateType): List<Template> {
        return templates.values.filter { it.templateType == templateType }
    }
    
    override fun deleteById(id: String): Boolean {
        return templates.remove(id) != null
    }
    
    override fun existsById(id: String): Boolean {
        return templates.containsKey(id)
    }
}
