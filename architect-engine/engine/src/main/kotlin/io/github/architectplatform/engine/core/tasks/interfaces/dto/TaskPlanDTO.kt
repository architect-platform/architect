package io.github.architectplatform.engine.core.tasks.interfaces.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class TaskPlanStepDTO(
    val id: String,
    val description: String,
    val phase: String?,
    val depends: List<String>,
    val batch: Int,
)

@Serdeable
data class TaskPlanDTO(
    val task: String,
    val project: String,
    val steps: List<TaskPlanStepDTO>,
    val totalSteps: Int = steps.size,
    val parallelBatches: Int = if (steps.isEmpty()) 0 else steps.maxOf { it.batch } + 1,
)
