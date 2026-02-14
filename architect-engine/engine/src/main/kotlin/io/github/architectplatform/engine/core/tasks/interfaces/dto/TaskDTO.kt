package io.github.architectplatform.engine.core.tasks.interfaces.dto

import io.github.architectplatform.api.core.tasks.Task
import io.micronaut.serde.annotation.Serdeable

/**
 * Enhanced DTO for task information with complete metadata.
 *
 * @property id Unique identifier of the task
 * @property description Human-readable description of the task
 * @property phase Phase this task belongs to (e.g., "BUILD", "TEST"), null if not phased
 * @property dependencies List of task IDs that this task depends on
 * @property children List of child task IDs that belong to this task
 */
@Serdeable
data class TaskDTO(
    val id: String,
    val description: String,
    val phase: String? = null,
    val dependencies: List<String> = emptyList(),
    val children: List<String> = emptyList()
)

fun Task.toDTO(): TaskDTO {
  return TaskDTO(
      id = id,
      description = description(),
      phase = phase()?.id,
      dependencies = depends(),
      children = children()
  )
}
