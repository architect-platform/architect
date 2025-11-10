package io.github.architectplatform.server.application.services

import io.github.architectplatform.server.application.domain.Port
import io.github.architectplatform.server.application.domain.ResourceDefinition
import io.github.architectplatform.server.application.domain.ResourceRequirements
import io.github.architectplatform.server.application.ports.inbound.ManageResourceDefinitionUseCase
import io.github.architectplatform.server.application.ports.outbound.ResourceDefinitionPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Service implementing resource definition management use cases.
 * Follows single responsibility principle - handles only resource definition operations.
 */
@Singleton
class ResourceDefinitionService(
    private val resourceDefinitionPort: ResourceDefinitionPort
) : ManageResourceDefinitionUseCase {

    private val logger = LoggerFactory.getLogger(ResourceDefinitionService::class.java)

    override fun createResourceDefinition(
        name: String,
        version: String,
        namespace: String,
        image: String?,
        replicas: Int,
        environmentVariables: Map<String, String>,
        ports: List<Port>,
        resources: ResourceRequirements?,
        labels: Map<String, String>,
        annotations: Map<String, String>,
        templateIds: List<String>
    ): ResourceDefinition {
        logger.info("Creating resource definition: $name version: $version")
        
        val resourceDefinition = ResourceDefinition(
            id = UUID.randomUUID().toString(),
            name = name,
            version = version,
            namespace = namespace,
            image = image,
            replicas = replicas,
            environmentVariables = environmentVariables,
            ports = ports,
            resources = resources,
            labels = labels,
            annotations = annotations,
            templateIds = templateIds
        )
        
        return resourceDefinitionPort.save(resourceDefinition).also {
            logger.info("Resource definition created with ID: ${it.id}")
        }
    }

    override fun updateResourceDefinition(
        resourceId: String,
        version: String?,
        image: String?,
        replicas: Int?,
        environmentVariables: Map<String, String>?,
        ports: List<Port>?,
        resources: ResourceRequirements?,
        labels: Map<String, String>?,
        annotations: Map<String, String>?,
        templateIds: List<String>?
    ): ResourceDefinition? {
        logger.info("Updating resource definition: $resourceId")
        
        val existing = resourceDefinitionPort.findById(resourceId) ?: run {
            logger.warn("Resource definition not found: $resourceId")
            return null
        }
        
        val updated = existing.update(
            version = version,
            image = image,
            replicas = replicas,
            environmentVariables = environmentVariables,
            ports = ports,
            resources = resources,
            labels = labels,
            annotations = annotations,
            templateIds = templateIds
        )
        
        return resourceDefinitionPort.save(updated).also {
            logger.info("Resource definition updated: $resourceId")
        }
    }

    override fun getResourceDefinition(resourceId: String): ResourceDefinition? {
        return resourceDefinitionPort.findById(resourceId)
    }

    override fun getAllResourceDefinitions(): List<ResourceDefinition> {
        return resourceDefinitionPort.findAll()
    }

    override fun getResourceDefinitionsByNamespace(namespace: String): List<ResourceDefinition> {
        return resourceDefinitionPort.findByNamespace(namespace)
    }

    override fun deleteResourceDefinition(resourceId: String): Boolean {
        logger.info("Deleting resource definition: $resourceId")
        return resourceDefinitionPort.deleteById(resourceId)
    }
}
