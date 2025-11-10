package io.github.architectplatform.agent.application

import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.JinjavaConfig
import io.github.architectplatform.agent.domain.DeploymentCommand
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service responsible for rendering Kubernetes YAML templates using Jinjava.
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
     * Render templates with provided variables
     */
    fun renderTemplates(command: DeploymentCommand): List<String> {
        logger.info("Rendering ${command.templates.size} template(s) for resource: ${command.resourceName}")

        return command.templates.mapIndexed { index, template ->
            try {
                val rendered = jinjava.render(template, command.variables)
                logger.debug("Successfully rendered template $index")
                rendered
            } catch (e: Exception) {
                logger.error("Failed to render template $index: ${e.message}", e)
                throw TemplateRenderingException("Failed to render template at index $index", e)
            }
        }
    }

    /**
     * Validate template syntax without rendering
     */
    fun validateTemplate(template: String, variables: Map<String, Any>): Boolean {
        return try {
            jinjava.render(template, variables)
            true
        } catch (e: Exception) {
            logger.error("Template validation failed: ${e.message}")
            false
        }
    }
}

class TemplateRenderingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
