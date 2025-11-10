package io.github.architectplatform.server.adapters.inbound.rest.dto

import io.github.architectplatform.server.application.domain.Template
import io.github.architectplatform.server.application.domain.TemplateType
import io.github.architectplatform.server.application.domain.TemplateVariable
import io.github.architectplatform.server.application.domain.VariableType
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateTemplateRequest(
    val name: String,
    val description: String? = null,
    val content: String,
    val templateType: TemplateType = TemplateType.DEPLOYMENT,
    val variables: List<TemplateVariableDTO> = emptyList()
)

@Serdeable
data class UpdateTemplateRequest(
    val name: String? = null,
    val description: String? = null,
    val content: String? = null,
    val templateType: TemplateType? = null,
    val variables: List<TemplateVariableDTO>? = null
)

@Serdeable
data class TemplateVariableDTO(
    val name: String,
    val description: String? = null,
    val defaultValue: String? = null,
    val required: Boolean = false,
    val type: VariableType = VariableType.STRING
) {
    fun toDomain() = TemplateVariable(
        name = name,
        description = description,
        defaultValue = defaultValue,
        required = required,
        type = type
    )
    
    companion object {
        fun fromDomain(variable: TemplateVariable) = TemplateVariableDTO(
            name = variable.name,
            description = variable.description,
            defaultValue = variable.defaultValue,
            required = variable.required,
            type = variable.type
        )
    }
}

@Serdeable
data class TemplateResponse(
    val id: String,
    val name: String,
    val description: String?,
    val content: String,
    val templateType: String,
    val variables: List<TemplateVariableDTO>,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromDomain(template: Template) = TemplateResponse(
            id = template.id,
            name = template.name,
            description = template.description,
            content = template.content,
            templateType = template.templateType.name,
            variables = template.variables.map { TemplateVariableDTO.fromDomain(it) },
            createdAt = template.createdAt.toString(),
            updatedAt = template.updatedAt.toString()
        )
    }
}

@Serdeable
data class ValidateTemplateRequest(
    val content: String,
    val variables: Map<String, Any>
)
