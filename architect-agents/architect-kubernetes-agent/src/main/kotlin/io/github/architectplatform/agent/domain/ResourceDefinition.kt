package io.github.architectplatform.agent.domain

/**
 * Domain model representing a Kubernetes resource definition.
 * This is what applications are defined as in Architect Server.
 */
data class ResourceDefinition(
    val name: String,
    val version: String,
    val image: String? = null,
    val replicas: Int = 1,
    val environmentVariables: Map<String, String> = emptyMap(),
    val ports: List<Port> = emptyList(),
    val resources: ResourceRequirements? = null
)

/**
 * Port configuration
 */
data class Port(
    val name: String,
    val containerPort: Int,
    val protocol: String = "TCP"
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
