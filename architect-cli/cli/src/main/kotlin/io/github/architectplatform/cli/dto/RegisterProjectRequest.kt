package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Request to register a project with the Architect Engine.
 *
 * @property name The name to assign to the project
 * @property path The file system path to the project root
 */
@Serdeable
data class RegisterProjectRequest(
    val name: String,
    val path: String,
)
