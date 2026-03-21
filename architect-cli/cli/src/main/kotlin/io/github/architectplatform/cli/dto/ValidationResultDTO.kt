package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ValidationResultDTO(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
)
