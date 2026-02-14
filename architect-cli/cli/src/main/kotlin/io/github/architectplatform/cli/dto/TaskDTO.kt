package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Enhanced data transfer object representing a task in the Architect Engine.
 *
 * @property id Unique identifier of the task (e.g., "build", "test", "deploy")
 * @property description Human-readable description of what the task does
 * @property phase Phase this task belongs to (e.g., "BUILD", "TEST"), null if not phased
 * @property dependencies List of task IDs that this task depends on
 * @property children List of child task IDs that belong to this task
 */
@Serdeable
data class TaskDTO(
    val id: String,
    val description: String = "",
    val phase: String? = null,
    val dependencies: List<String> = emptyList(),
    val children: List<String> = emptyList()
) {
  override fun toString(): String {
    return id
  }
}
