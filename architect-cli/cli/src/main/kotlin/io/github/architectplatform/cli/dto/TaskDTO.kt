package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Data transfer object representing a task in the Architect Engine.
 *
 * @property id Unique identifier of the task (e.g., "build", "test", "deploy")
 * @property description Human-readable description of the task
 * @property phase Phase the task belongs to, if any
 */
@Serdeable
data class TaskDTO(
    val id: String,
    val description: String = "",
    val phase: String? = null,
) {
  override fun toString(): String {
    return id
  }
}
