package io.github.architectplatform.server.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Resource definition for creating Kubernetes applications
 */
data class ResourceDefinitionDTO(
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("version")
    val version: String,
    
    @JsonProperty("namespace")
    val namespace: String = "default",
    
    @JsonProperty("image")
    val image: String? = null,
    
    @JsonProperty("replicas")
    val replicas: Int = 1,
    
    @JsonProperty("environmentVariables")
    val environmentVariables: Map<String, String> = emptyMap(),
    
    @JsonProperty("ports")
    val ports: List<PortDTO> = emptyList(),
    
    @JsonProperty("resources")
    val resources: ResourceRequirementsDTO? = null,
    
    @JsonProperty("labels")
    val labels: Map<String, String> = emptyMap(),
    
    @JsonProperty("annotations")
    val annotations: Map<String, String> = emptyMap()
)

/**
 * Port configuration
 */
data class PortDTO(
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("containerPort")
    val containerPort: Int,
    
    @JsonProperty("protocol")
    val protocol: String = "TCP",
    
    @JsonProperty("servicePort")
    val servicePort: Int? = null
)

/**
 * Resource requirements (CPU/Memory)
 */
data class ResourceRequirementsDTO(
    @JsonProperty("requests")
    val requests: ResourceSpecDTO? = null,
    
    @JsonProperty("limits")
    val limits: ResourceSpecDTO? = null
)

/**
 * Resource specification
 */
data class ResourceSpecDTO(
    @JsonProperty("cpu")
    val cpu: String? = null,
    
    @JsonProperty("memory")
    val memory: String? = null
)
