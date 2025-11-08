package io.github.architectplatform.engine.core.auth

import io.github.architectplatform.engine.core.auth.dto.AuthResponse
import io.github.architectplatform.engine.core.auth.dto.AuthStatusResponse
import io.github.architectplatform.engine.core.auth.dto.SetGitHubTokenRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory

/**
 * REST API controller for authentication operations.
 * 
 * Provides endpoints for managing GitHub authentication tokens.
 */
@Controller("/auth")
class AuthController(
    private val authConfigManager: AuthConfigManager
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Sets the GitHub token for authenticated API requests.
     * 
     * @param request The request containing the GitHub token
     * @return Response indicating success or failure
     */
    @Post("/github")
    fun setGitHubToken(@Body request: SetGitHubTokenRequest): AuthResponse {
        return try {
            if (request.token.isBlank()) {
                return AuthResponse(
                    success = false,
                    message = "Token cannot be empty"
                )
            }
            
            authConfigManager.setGitHubToken(request.token)
            logger.info("GitHub authentication configured successfully")
            
            AuthResponse(
                success = true,
                message = "GitHub token stored successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to store GitHub token: ${e.message}")
            AuthResponse(
                success = false,
                message = "Failed to store token: ${e.message}"
            )
        }
    }
    
    /**
     * Checks the authentication status for GitHub.
     * 
     * @return Status indicating if authenticated
     */
    @Get("/github/status")
    fun getGitHubStatus(): AuthStatusResponse {
        return AuthStatusResponse(
            authenticated = authConfigManager.hasGitHubToken(),
            provider = "github"
        )
    }
    
    /**
     * Removes the stored GitHub token.
     * 
     * @return Response indicating success or failure
     */
    @Delete("/github")
    fun clearGitHubToken(): AuthResponse {
        return try {
            authConfigManager.clearGitHubToken()
            logger.info("GitHub authentication cleared")
            
            AuthResponse(
                success = true,
                message = "GitHub token removed successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to clear GitHub token: ${e.message}")
            AuthResponse(
                success = false,
                message = "Failed to clear token: ${e.message}"
            )
        }
    }
}
