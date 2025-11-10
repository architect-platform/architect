package io.github.architectplatform.data.application.domain

import java.time.Instant

/**
 * Domain model representing a project registered for execution tracking.
 * Pure domain object without infrastructure concerns.
 */
data class Project(
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now()
)
