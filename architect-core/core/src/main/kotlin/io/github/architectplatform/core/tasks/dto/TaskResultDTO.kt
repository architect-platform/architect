package io.github.architectplatform.core.tasks.dto

import io.github.architectplatform.api.core.tasks.TaskMetadata
import io.github.architectplatform.api.core.tasks.TaskResult

data class TaskMetadataDTO(
    val durationMs: Long?,
    val exitCode: Int?,
    val startedAt: String?,
    val finishedAt: String?,
    val executorInfo: String?,
)

data class TaskResultDTO(
    val success: Boolean,
    val message: String?,
    val subResults: List<TaskResultDTO> = emptyList(),
    val metadata: TaskMetadataDTO? = null,
    val data: Map<String, Any> = emptyMap(),
)

fun TaskMetadata.toDTO(): TaskMetadataDTO =
    TaskMetadataDTO(
        durationMs = duration?.toMillis(),
        exitCode = exitCode,
        startedAt = startedAt?.toString(),
        finishedAt = finishedAt?.toString(),
        executorInfo = executorInfo,
    )

fun TaskResult.toDTO(): TaskResultDTO {
  return TaskResultDTO(
      success = success,
      message = message,
      subResults = results.map { it.toDTO() },
      metadata = metadata?.toDTO(),
      data = data,
  )
}
