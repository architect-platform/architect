package io.github.architectplatform.server.application.services

import io.github.architectplatform.server.application.domain.*
import io.github.architectplatform.server.application.ports.inbound.ManageApplicationDefinitionUseCase
import io.github.architectplatform.server.application.ports.outbound.ApplicationDefinitionPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Service for managing platform-agnostic application definitions.
 * Clean, simple service following single responsibility principle.
 */
@Singleton
class ApplicationDefinitionService(
    private val applicationDefinitionPort: ApplicationDefinitionPort
) : ManageApplicationDefinitionUseCase {

    private val logger = LoggerFactory.getLogger(ApplicationDefinitionService::class.java)

    override fun createApplicationDefinition(
        name: String,
        version: String,
        type: ApplicationType,
        image: String,
        instances: Int,
        environment: Map<String, String>,
        exposedPorts: List<ExposedPort>,
        resources: ResourceLimits?,
        healthCheck: HealthCheck?,
        metadata: Map<String, String>,
        dependencies: List<Dependency>,
        templateIds: List<String>
    ): ApplicationDefinition {
        logger.info("Creating application definition: $name version: $version type: $type")
        
        val appDefinition = ApplicationDefinition(
            id = UUID.randomUUID().toString(),
            name = name,
            version = version,
            type = type,
            image = image,
            instances = instances,
            environment = environment,
            exposedPorts = exposedPorts,
            resources = resources,
            healthCheck = healthCheck,
            metadata = metadata,
            dependencies = dependencies,
            templateIds = templateIds
        )
        
        return applicationDefinitionPort.save(appDefinition).also {
            logger.info("Application definition created with ID: ${it.id}")
        }
    }

    override fun updateApplicationDefinition(
        appId: String,
        version: String?,
        type: ApplicationType?,
        image: String?,
        instances: Int?,
        environment: Map<String, String>?,
        exposedPorts: List<ExposedPort>?,
        resources: ResourceLimits?,
        healthCheck: HealthCheck?,
        metadata: Map<String, String>?,
        dependencies: List<Dependency>?,
        templateIds: List<String>?
    ): ApplicationDefinition? {
        logger.info("Updating application definition: $appId")
        
        val existing = applicationDefinitionPort.findById(appId) ?: run {
            logger.warn("Application definition not found: $appId")
            return null
        }
        
        val updated = existing.update(
            version = version,
            type = type,
            image = image,
            instances = instances,
            environment = environment,
            exposedPorts = exposedPorts,
            resources = resources,
            healthCheck = healthCheck,
            metadata = metadata,
            dependencies = dependencies,
            templateIds = templateIds
        )
        
        return applicationDefinitionPort.save(updated).also {
            logger.info("Application definition updated: $appId")
        }
    }

    override fun getApplicationDefinition(appId: String): ApplicationDefinition? {
        return applicationDefinitionPort.findById(appId)
    }

    override fun getAllApplicationDefinitions(): List<ApplicationDefinition> {
        return applicationDefinitionPort.findAll()
    }

    override fun deleteApplicationDefinition(appId: String): Boolean {
        logger.info("Deleting application definition: $appId")
        return applicationDefinitionPort.deleteById(appId)
    }
}
