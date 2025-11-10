package io.github.architectplatform.server.application.domain

import java.time.Instant

/**
 * Domain model representing an event that occurred during an execution.
 * Pure domain object without infrastructure concerns.
 */
data class ExecutionEvent(
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String? = null,
    val message: String? = null,
    val output: String? = null,
    val success: Boolean = true,
    val timestamp: Instant = Instant.now()
)
