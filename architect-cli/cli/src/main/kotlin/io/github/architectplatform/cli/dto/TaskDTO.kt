package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Data transfer object representing a task in the Architect Engine.
 *
 * @property id Unique identifier of the task (e.g., "build", "test", "deploy")
 */
@Serdeable
data class TaskDTO(
    val id: String,
) {
  override fun toString(): String {
    return id
  }
}
