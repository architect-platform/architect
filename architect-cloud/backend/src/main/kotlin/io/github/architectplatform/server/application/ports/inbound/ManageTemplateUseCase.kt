package io.github.architectplatform.server.application.ports.inbound

import io.github.architectplatform.server.application.domain.Template
import io.github.architectplatform.server.application.domain.TemplateType
import io.github.architectplatform.server.application.domain.TemplateVariable

/**
 * Use case interface for managing templates.
 * Inbound port defining what the application can do with templates.
 */
interface ManageTemplateUseCase {
    
    /**
     * Create a new template
     */
    fun createTemplate(
        name: String,
        description: String?,
        content: String,
        templateType: TemplateType,
        variables: List<TemplateVariable>
    ): Template
    
    /**
     * Update an existing template
     */
    fun updateTemplate(
        templateId: String,
        name: String? = null,
        description: String? = null,
        content: String? = null,
        templateType: TemplateType? = null,
        variables: List<TemplateVariable>? = null
    ): Template?
    
    /**
     * Get a template by ID
     */
    fun getTemplate(templateId: String): Template?
    
    /**
     * Get all templates
     */
    fun getAllTemplates(): List<Template>
    
    /**
     * Get templates by type
     */
    fun getTemplatesByType(templateType: TemplateType): List<Template>
    
    /**
     * Delete a template
     */
    fun deleteTemplate(templateId: String): Boolean
    
    /**
     * Validate template syntax
     */
    fun validateTemplate(content: String, variables: Map<String, Any>): Boolean
}
