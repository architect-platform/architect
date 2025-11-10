package io.github.architectplatform.agent.common.service

import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.JinjavaConfig
import org.slf4j.LoggerFactory

/**
 * Common template rendering service using Jinja2.
 * Shared across all agent types.
 */
class TemplateRenderingService {

    private val logger = LoggerFactory.getLogger(TemplateRenderingService::class.java)
    private val jinjava: Jinjava

    init {
        val config = JinjavaConfig.newBuilder()
            .withFailOnUnknownTokens(true)
            .build()
        jinjava = Jinjava(config)
    }

    /**
     * Render a single template with the given variables.
     */
    fun renderTemplate(template: String, variables: Map<String, Any>): String {
        return try {
            logger.debug("Rendering template with ${variables.size} variables")
            jinjava.render(template, variables)
        } catch (e: Exception) {
            logger.error("Failed to render template: ${e.message}", e)
            throw TemplateRenderingException("Template rendering failed: ${e.message}", e)
        }
    }

    /**
     * Render multiple templates and return them as a list.
     */
    fun renderTemplates(templates: List<String>, variables: Map<String, Any>): List<String> {
        return templates.mapIndexed { index, template ->
            try {
                renderTemplate(template, variables)
            } catch (e: Exception) {
                logger.error("Failed to render template $index: ${e.message}")
                throw TemplateRenderingException("Failed to render template at index $index", e)
            }
        }
    }

    /**
     * Render templates and merge into a single output with separator.
     */
    fun renderAndMerge(templates: List<String>, variables: Map<String, Any>, separator: String = "\n---\n"): String {
        val rendered = renderTemplates(templates, variables)
        return rendered.joinToString(separator)
    }

    /**
     * Validate a template without rendering it fully.
     */
    fun validateTemplate(template: String): Boolean {
        return try {
            jinjava.render(template, emptyMap())
            true
        } catch (e: Exception) {
            logger.warn("Template validation failed: ${e.message}")
            false
        }
    }
}

/**
 * Exception thrown when template rendering fails.
 */
class TemplateRenderingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
