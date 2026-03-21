package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class HistoryRecordDTO(
    val id: String,
    val project: String,
    val task: String,
    val timestamp: Long,
    val success: Boolean,
    val durationMs: Long,
    val message: String?,
)
