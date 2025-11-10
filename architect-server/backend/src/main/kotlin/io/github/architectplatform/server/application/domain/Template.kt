package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model representing a Kubernetes YAML template.
 * Pure domain object without infrastructure concerns.
 */
data class Template(
    val id: String,
    val name: String,
    val description: String? = null,
    val content: String,
    val templateType: TemplateType,
    val variables: List<TemplateVariable> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun update(
        name: String? = null,
        description: String? = null,
        content: String? = null,
        templateType: TemplateType? = null,
        variables: List<TemplateVariable>? = null
    ): Template {
        return copy(
            name = name ?: this.name,
            description = description ?: this.description,
            content = content ?: this.content,
            templateType = templateType ?: this.templateType,
            variables = variables ?: this.variables,
            updatedAt = Instant.now()
        )
    }
}

/**
 * Template variable definition
 */
data class TemplateVariable(
    val name: String,
    val description: String? = null,
    val defaultValue: String? = null,
    val required: Boolean = false,
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
