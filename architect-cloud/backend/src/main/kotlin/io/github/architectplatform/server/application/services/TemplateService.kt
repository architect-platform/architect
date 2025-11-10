package io.github.architectplatform.server.application.services

import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.JinjavaConfig
import io.github.architectplatform.server.application.domain.Template
import io.github.architectplatform.server.application.domain.TemplateType
import io.github.architectplatform.server.application.domain.TemplateVariable
import io.github.architectplatform.server.application.ports.inbound.ManageTemplateUseCase
import io.github.architectplatform.server.application.ports.outbound.TemplatePort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Service implementing template management use cases.
 * Follows single responsibility principle - handles only template operations.
 */
@Singleton
class TemplateService(
    private val templatePort: TemplatePort
) : ManageTemplateUseCase {

    private val logger = LoggerFactory.getLogger(TemplateService::class.java)
    private val jinjava: Jinjava

    init {
        val config = JinjavaConfig.newBuilder()
            .withFailOnUnknownTokens(true)
            .build()
        jinjava = Jinjava(config)
    }

    override fun createTemplate(
        name: String,
        description: String?,
        content: String,
        templateType: TemplateType,
        variables: List<TemplateVariable>
    ): Template {
        logger.info("Creating template: $name of type: $templateType")
        
        val template = Template(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            content = content,
            templateType = templateType,
            variables = variables
        )
        
        return templatePort.save(template).also {
            logger.info("Template created with ID: ${it.id}")
        }
    }

    override fun updateTemplate(
        templateId: String,
        name: String?,
        description: String?,
        content: String?,
        templateType: TemplateType?,
        variables: List<TemplateVariable>?
    ): Template? {
        logger.info("Updating template: $templateId")
        
        val existing = templatePort.findById(templateId) ?: run {
            logger.warn("Template not found: $templateId")
            return null
        }
        
        val updated = existing.update(
            name = name,
            description = description,
            content = content,
            templateType = templateType,
            variables = variables
        )
        
        return templatePort.save(updated).also {
            logger.info("Template updated: $templateId")
        }
    }

    override fun getTemplate(templateId: String): Template? {
        return templatePort.findById(templateId)
    }

    override fun getAllTemplates(): List<Template> {
        return templatePort.findAll()
    }

    override fun getTemplatesByType(templateType: TemplateType): List<Template> {
        return templatePort.findByType(templateType)
    }

    override fun deleteTemplate(templateId: String): Boolean {
        logger.info("Deleting template: $templateId")
        return templatePort.deleteById(templateId)
    }

    override fun validateTemplate(content: String, variables: Map<String, Any>): Boolean {
        return try {
            jinjava.render(content, variables)
            true
        } catch (e: Exception) {
            logger.error("Template validation failed: ${e.message}")
            false
        }
    }
}
