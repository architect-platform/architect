package io.github.architectplatform.cloud.application.domain

import java.time.Instant

/**
 * Domain model representing a project registered in the cloud.
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
