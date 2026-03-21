package io.github.architectplatform.engine.core.project.interfaces.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ValidationResultDTO(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
)
