package io.github.architectplatform.agent.dockercompose.application

import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.JinjavaConfig
import io.github.architectplatform.agent.dockercompose.domain.DeploymentCommand
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service for rendering Docker Compose templates.
 * Follows Single Responsibility Principle - only handles template rendering.
 */
@Singleton
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
     * Render templates and merge into single docker-compose.yml.
     * Applies template method pattern - defines algorithm skeleton.
     */
    fun renderDockerCompose(command: DeploymentCommand): String {
        logger.info("Rendering docker-compose templates for: ${command.resourceName}")

        return try {
            val renderedTemplates = command.templates.mapIndexed { index, template ->
                renderSingleTemplate(template, command.variables, index)
            }
            
            // Merge templates into single docker-compose file
            mergeTemplates(renderedTemplates)
        } catch (e: Exception) {
            logger.error("Failed to render templates: ${e.message}", e)
            throw TemplateRenderingException("Template rendering failed", e)
        }
    }

    private fun renderSingleTemplate(template: String, variables: Map<String, Any>, index: Int): String {
        return try {
            jinjava.render(template, variables)
        } catch (e: Exception) {
            logger.error("Failed to render template $index: ${e.message}")
            throw TemplateRenderingException("Failed to render template at index $index", e)
        }
    }

    private fun mergeTemplates(templates: List<String>): String {
        // Simple merge strategy - concatenate with proper YAML formatting
        return templates.joinToString("\n---\n")
    }
}

class TemplateRenderingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
