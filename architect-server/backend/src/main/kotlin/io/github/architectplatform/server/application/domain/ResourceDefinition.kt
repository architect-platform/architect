package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model representing an application resource definition.
 * Defines what should be deployed to Kubernetes.
 */
data class ResourceDefinition(
    val id: String,
    val name: String,
    val version: String,
    val namespace: String = "default",
    val image: String? = null,
    val replicas: Int = 1,
    val environmentVariables: Map<String, String> = emptyMap(),
    val ports: List<Port> = emptyList(),
    val resources: ResourceRequirements? = null,
    val labels: Map<String, String> = emptyMap(),
    val annotations: Map<String, String> = emptyMap(),
    val templateIds: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun update(
        version: String? = null,
        image: String? = null,
        replicas: Int? = null,
        environmentVariables: Map<String, String>? = null,
        ports: List<Port>? = null,
        resources: ResourceRequirements? = null,
        labels: Map<String, String>? = null,
        annotations: Map<String, String>? = null,
        templateIds: List<String>? = null
    ): ResourceDefinition {
        return copy(
            version = version ?: this.version,
            image = image ?: this.image,
            replicas = replicas ?: this.replicas,
            environmentVariables = environmentVariables ?: this.environmentVariables,
            ports = ports ?: this.ports,
            resources = resources ?: this.resources,
            labels = labels ?: this.labels,
            annotations = annotations ?: this.annotations,
            templateIds = templateIds ?: this.templateIds,
            updatedAt = Instant.now()
        )
    }

    fun toVariableMap(): Map<String, Any> {
        val vars = mutableMapOf<String, Any>(
            "name" to name,
            "version" to version,
            "namespace" to namespace,
            "replicas" to replicas,
            "env" to environmentVariables,
            "labels" to labels,
            "annotations" to annotations
        )

        image?.let { vars["image"] = it }

        if (ports.isNotEmpty()) {
            vars["ports"] = ports.map {
                mapOf(
                    "name" to it.name,
                    "containerPort" to it.containerPort,
                    "protocol" to it.protocol,
                    "servicePort" to (it.servicePort ?: it.containerPort)
                )
            }
        }

        resources?.let { res ->
            val resourcesMap = mutableMapOf<String, Any>()
            res.requests?.let {
                resourcesMap["requests"] = mapOf(
                    "cpu" to (it.cpu ?: ""),
                    "memory" to (it.memory ?: "")
                ).filterValues { v -> v.isNotEmpty() }
            }
            res.limits?.let {
                resourcesMap["limits"] = mapOf(
                    "cpu" to (it.cpu ?: ""),
                    "memory" to (it.memory ?: "")
                ).filterValues { v -> v.isNotEmpty() }
            }
            if (resourcesMap.isNotEmpty()) {
                vars["resources"] = resourcesMap
            }
        }

        return vars
    }
}

/**
 * Port configuration
 */
data class Port(
    val name: String,
    val containerPort: Int,
    val protocol: String = "TCP",
    val servicePort: Int? = null
)

/**
 * Resource requirements (CPU/Memory)
 */
data class ResourceRequirements(
    val requests: ResourceSpec? = null,
    val limits: ResourceSpec? = null
)

/**
 * Resource specification
 */
data class ResourceSpec(
    val cpu: String? = null,
    val memory: String? = null
)
