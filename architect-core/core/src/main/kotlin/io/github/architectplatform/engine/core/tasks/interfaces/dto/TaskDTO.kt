package io.github.architectplatform.engine.core.tasks.interfaces.dto

import io.github.architectplatform.api.core.tasks.Task

data class TaskDTO(val id: String, val description: String)

fun Task.toDTO(): TaskDTO {
  return TaskDTO(id = id, description = description())
}
