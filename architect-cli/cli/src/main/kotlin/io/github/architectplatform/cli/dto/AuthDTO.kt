package io.github.architectplatform.cli.dto

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
    val provider: String
)

/**
 * Generic response for authentication operations.
 */
@Serdeable
data class AuthResponse(
    val success: Boolean,
    val message: String
)
