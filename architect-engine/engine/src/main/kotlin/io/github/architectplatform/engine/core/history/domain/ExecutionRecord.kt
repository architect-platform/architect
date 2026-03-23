package io.github.architectplatform.engine.core.history.domain

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ExecutionRecord(
    val id: String,
    val project: String,
    val task: String,
    val timestamp: Long,
    val success: Boolean,
    val durationMs: Long,
    val message: String?,
    val user: String? = null,
    val args: List<String> = emptyList(),
    val result: String? = null,
)
