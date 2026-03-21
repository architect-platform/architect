package io.github.architectplatform.cli.dto

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
    val totalSteps: Int,
    val parallelBatches: Int,
)
