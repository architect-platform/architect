package io.github.architectplatform.engine.core.auth

import io.github.architectplatform.engine.core.auth.dto.AuthResponse
import io.github.architectplatform.engine.core.auth.dto.AuthStatusResponse
import io.github.architectplatform.engine.core.auth.dto.DeviceFlowInitResponse
import io.github.architectplatform.engine.core.auth.dto.PollTokenRequest
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
 * Provides endpoints for managing GitHub authentication tokens and OAuth flows.
 */
@Controller("/auth")
class AuthController(
    private val authConfigManager: AuthConfigManager,
    private val gitHubAuthService: GitHubAuthService
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
    
    /**
     * Initiates the GitHub OAuth device flow.
     * 
     * @return Device flow initialization response with user code and verification URI
     */
    @Post("/github/device-flow/init")
    fun initiateDeviceFlow(): DeviceFlowInitResponse {
        return try {
            val response = gitHubAuthService.initiateDeviceFlow()
            logger.info("Device flow initiated")
            
            DeviceFlowInitResponse(
                success = true,
                deviceCode = response.device_code,
                userCode = response.user_code,
                verificationUri = response.verification_uri,
                expiresIn = response.expires_in,
                interval = response.interval
            )
        } catch (e: Exception) {
            logger.error("Failed to initiate device flow: ${e.message}")
            DeviceFlowInitResponse(
                success = false,
                errorMessage = "Failed to initiate device flow: ${e.message}"
            )
        }
    }
    
    /**
     * Polls for the access token during device flow.
     * 
     * @param request Poll request containing the device code
     * @return Response indicating if token was obtained
     */
    @Post("/github/device-flow/poll")
    fun pollForToken(@Body request: PollTokenRequest): AuthResponse {
        return try {
            val token = gitHubAuthService.pollForAccessToken(request.deviceCode, request.interval)
            
            if (token != null) {
                AuthResponse(
                    success = true,
                    message = "Authentication successful"
                )
            } else {
                AuthResponse(
                    success = false,
                    message = "Authorization pending"
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to poll for token: ${e.message}")
            AuthResponse(
                success = false,
                message = "Failed to complete authentication: ${e.message}"
            )
        }
    }
    
    /**
     * Verifies a GitHub token.
     * 
     * @param request Request containing the token to verify
     * @return Response with user information if valid
     */
    @Post("/github/verify")
    fun verifyGitHubToken(@Body request: SetGitHubTokenRequest): AuthResponse {
        return try {
            val user = gitHubAuthService.verifyToken(request.token)
            logger.info("Token verified for user: ${user.login}")
            
            AuthResponse(
                success = true,
                message = "Token valid for user: ${user.login}"
            )
        } catch (e: Exception) {
            logger.error("Token verification failed: ${e.message}")
            AuthResponse(
                success = false,
                message = "Invalid token: ${e.message}"
            )
        }
    }
}
