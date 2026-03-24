package io.github.architectplatform.core.tasks.interfaces.dto


data class TaskPlanStepDTO(
    val id: String,
    val description: String,
    val phase: String?,
    val depends: List<String>,
    val batch: Int,
)

data class TaskPlanDTO(
    val task: String,
    val project: String,
    val steps: List<TaskPlanStepDTO>,
    val totalSteps: Int = steps.size,
    val parallelBatches: Int = if (steps.isEmpty()) 0 else steps.maxOf { it.batch } + 1,
)
