package io.github.architectplatform.server.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Kubernetes YAML template
 */
data class TemplateDTO(
    @JsonProperty("id")
    val id: String,
    
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("description")
    val description: String? = null,
    
    @JsonProperty("content")
    val content: String,
    
    @JsonProperty("templateType")
    val templateType: TemplateType = TemplateType.DEPLOYMENT,
    
    @JsonProperty("variables")
    val variables: List<TemplateVariableDTO> = emptyList(),
    
    @JsonProperty("createdAt")
    val createdAt: Instant = Instant.now(),
    
    @JsonProperty("updatedAt")
    val updatedAt: Instant = Instant.now()
)

/**
 * Template variable definition
 */
data class TemplateVariableDTO(
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("description")
    val description: String? = null,
    
    @JsonProperty("defaultValue")
    val defaultValue: String? = null,
    
    @JsonProperty("required")
    val required: Boolean = false,
    
    @JsonProperty("type")
    val type: VariableType = VariableType.STRING
)

/**
 * Type of Kubernetes template
 */
enum class TemplateType {
    DEPLOYMENT,
    SERVICE,
    INGRESS,
    CONFIG_MAP,
    SECRET,
    STATEFUL_SET,
    DAEMON_SET,
    JOB,
    CRON_JOB,
    CUSTOM
}

/**
 * Variable type
 */
enum class VariableType {
    STRING,
    NUMBER,
    BOOLEAN,
    LIST,
    MAP
}

/**
 * Request to create or update a template
 */
data class CreateTemplateRequest(
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("description")
    val description: String? = null,
    
    @JsonProperty("content")
    val content: String,
    
    @JsonProperty("templateType")
    val templateType: TemplateType = TemplateType.DEPLOYMENT,
    
    @JsonProperty("variables")
    val variables: List<TemplateVariableDTO> = emptyList()
)
