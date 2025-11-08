package io.github.architectplatform.engine.core.auth.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Request to set a GitHub token.
 */
@Serdeable
data class SetGitHubTokenRequest(
    val token: String
)

/**
 * Response for authentication status.
 */
@Serdeable
data class AuthStatusResponse(
    val authenticated: Boolean,
    val provider: String = "github"
)

/**
 * Generic response for authentication operations.
 */
@Serdeable
data class AuthResponse(
    val success: Boolean,
    val message: String
)

/**
 * Response for device flow initialization.
 */
@Serdeable
data class DeviceFlowInitResponse(
    val success: Boolean,
    val deviceCode: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val expiresIn: Int? = null,
    val interval: Int? = null,
    val errorMessage: String? = null
)

/**
 * Request to poll for access token during device flow.
 */
@Serdeable
data class PollTokenRequest(
    val deviceCode: String,
    val interval: Int = 5
)
