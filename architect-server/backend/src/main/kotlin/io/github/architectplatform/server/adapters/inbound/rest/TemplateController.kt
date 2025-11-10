package io.github.architectplatform.server.adapters.inbound.rest

import io.github.architectplatform.server.adapters.inbound.rest.dto.*
import io.github.architectplatform.server.application.domain.TemplateType
import io.github.architectplatform.server.application.ports.inbound.ManageTemplateUseCase
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

/**
 * REST controller for template management.
 * Follows REST API best practices with proper HTTP methods and status codes.
 */
@Controller("/api/templates")
@ExecuteOn(TaskExecutors.IO)
class TemplateController(
    private val manageTemplateUseCase: ManageTemplateUseCase
) {

    @Post
    fun createTemplate(@Body request: CreateTemplateRequest): TemplateResponse {
        val template = manageTemplateUseCase.createTemplate(
            name = request.name,
            description = request.description,
            content = request.content,
            templateType = request.templateType,
            variables = request.variables.map { it.toDomain() }
        )
        return TemplateResponse.fromDomain(template)
    }

    @Put("/{templateId}")
    fun updateTemplate(
        @PathVariable templateId: String,
        @Body request: UpdateTemplateRequest
    ): TemplateResponse? {
        val template = manageTemplateUseCase.updateTemplate(
            templateId = templateId,
            name = request.name,
            description = request.description,
            content = request.content,
            templateType = request.templateType,
            variables = request.variables?.map { it.toDomain() }
        )
        return template?.let { TemplateResponse.fromDomain(it) }
    }

    @Get("/{templateId}")
    fun getTemplate(@PathVariable templateId: String): TemplateResponse? {
        return manageTemplateUseCase.getTemplate(templateId)
            ?.let { TemplateResponse.fromDomain(it) }
    }

    @Get
    fun getAllTemplates(): List<TemplateResponse> {
        return manageTemplateUseCase.getAllTemplates()
            .map { TemplateResponse.fromDomain(it) }
    }

    @Get("/type/{templateType}")
    fun getTemplatesByType(@PathVariable templateType: TemplateType): List<TemplateResponse> {
        return manageTemplateUseCase.getTemplatesByType(templateType)
            .map { TemplateResponse.fromDomain(it) }
    }

    @Delete("/{templateId}")
    fun deleteTemplate(@PathVariable templateId: String): Map<String, Boolean> {
        val deleted = manageTemplateUseCase.deleteTemplate(templateId)
        return mapOf("deleted" to deleted)
    }

    @Post("/validate")
    fun validateTemplate(@Body request: ValidateTemplateRequest): Map<String, Boolean> {
        val valid = manageTemplateUseCase.validateTemplate(
            content = request.content,
            variables = request.variables
        )
        return mapOf("valid" to valid)
    }
}
