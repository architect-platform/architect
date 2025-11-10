package io.github.architectplatform.agent.application

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.github.architectplatform.agent.domain.AppliedResource
import io.github.architectplatform.agent.domain.DeploymentCommand
import io.github.architectplatform.agent.domain.DeploymentResult
import io.github.architectplatform.agent.domain.DeploymentStatus
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

/**
 * Service responsible for applying Kubernetes manifests to the cluster.
 */
@Singleton
class KubernetesDeploymentService(
    private val templateRenderingService: TemplateRenderingService
) {

    private val logger = LoggerFactory.getLogger(KubernetesDeploymentService::class.java)
    private val kubernetesClient: KubernetesClient = KubernetesClientBuilder().build()

    /**
     * Deploy a resource to Kubernetes cluster
     */
    fun deploy(command: DeploymentCommand): DeploymentResult {
        logger.info("Starting deployment for resource: ${command.resourceName} in namespace: ${command.namespace}")

        try {
            // Render templates
            val renderedManifests = templateRenderingService.renderTemplates(command)
            var deploymentResult = command.withRenderedManifests(renderedManifests)

            // Ensure namespace exists
            ensureNamespaceExists(command.namespace)

            // Apply each manifest
            val appliedResources = mutableListOf<AppliedResource>()
            deploymentResult = deploymentResult.copy(status = DeploymentStatus.DEPLOYING)

            renderedManifests.forEach { manifest ->
                val resources = applyManifest(manifest, command.namespace)
                appliedResources.addAll(resources)
            }

            logger.info("Successfully deployed ${appliedResources.size} resource(s) for: ${command.resourceName}")
            return deploymentResult.complete(
                resources = appliedResources,
                message = "Successfully deployed ${appliedResources.size} Kubernetes resource(s)"
            )

        } catch (e: Exception) {
            logger.error("Deployment failed for resource: ${command.resourceName}", e)
            return command.withRenderedManifests(emptyList())
                .fail("Deployment failed: ${e.message}")
        }
    }

    /**
     * Apply a single manifest to Kubernetes
     */
    private fun applyManifest(manifest: String, namespace: String): List<AppliedResource> {
        val appliedResources = mutableListOf<AppliedResource>()

        try {
            val resources: List<HasMetadata> = kubernetesClient
                .load(ByteArrayInputStream(manifest.toByteArray()))
                .items()

            resources.forEach { resource ->
                // Set namespace if not already set
                if (resource.metadata.namespace == null) {
                    resource.metadata.namespace = namespace
                }

                // Apply resource (server-side apply)
                val applied = kubernetesClient.resource(resource)
                    .inNamespace(namespace)
                    .createOrReplace()

                appliedResources.add(
                    AppliedResource(
                        kind = applied.kind,
                        name = applied.metadata.name,
                        namespace = applied.metadata.namespace ?: namespace,
                        apiVersion = applied.apiVersion
                    )
                )

                logger.info("Applied ${applied.kind}/${applied.metadata.name} in namespace $namespace")
            }

        } catch (e: Exception) {
            logger.error("Failed to apply manifest", e)
            throw KubernetesDeploymentException("Failed to apply manifest to cluster", e)
        }

        return appliedResources
    }

    /**
     * Ensure the namespace exists, create if it doesn't
     */
    private fun ensureNamespaceExists(namespace: String) {
        try {
            val ns = kubernetesClient.namespaces().withName(namespace).get()
            if (ns == null) {
                logger.info("Creating namespace: $namespace")
                // Create namespace using YAML string
                val namespaceYaml = """
                    apiVersion: v1
                    kind: Namespace
                    metadata:
                      name: $namespace
                """.trimIndent()
                kubernetesClient.load(ByteArrayInputStream(namespaceYaml.toByteArray())).create()
                logger.info("Namespace created: $namespace")
            } else {
                logger.debug("Namespace already exists: $namespace")
            }
        } catch (e: Exception) {
            logger.warn("Could not ensure namespace exists: ${e.message}")
            // Don't fail deployment if namespace operations fail
        }
    }

    /**
     * Get deployment status for a resource
     */
    fun getDeploymentStatus(resourceName: String, namespace: String): Map<String, Any> {
        // This would check actual Kubernetes resource status
        // For now, return a simple status map
        return mapOf(
            "resourceName" to resourceName,
            "namespace" to namespace,
            "exists" to true
        )
    }
}

class KubernetesDeploymentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
