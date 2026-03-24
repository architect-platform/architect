package io.github.architectplatform.core.tasks.interfaces.dto

import io.github.architectplatform.api.core.tasks.TaskResult

data class TaskResultDTO(
    val success: Boolean,
    val message: String?,
    val subResults: List<TaskResultDTO> = emptyList(),
)

fun TaskResult.toDTO(): TaskResultDTO {
  return TaskResultDTO(
      success = success, message = message, subResults = results.map { it.toDTO() })
}
