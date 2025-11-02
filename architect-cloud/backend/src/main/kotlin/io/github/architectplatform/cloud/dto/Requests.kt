package io.github.architectplatform.cloud.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class RegisterEngineRequest(
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?
)

@Serdeable
data class RegisterProjectRequest(
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String? = null
)

@Serdeable
data class ReportExecutionRequest(
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: String,
    val message: String? = null,
    val errorDetails: String? = null
)

@Serdeable
data class ReportEventRequest(
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String? = null,
    val message: String? = null,
    val output: String? = null,
    val success: Boolean = true
)

@Serdeable
data class HeartbeatRequest(
    val engineId: String
)
