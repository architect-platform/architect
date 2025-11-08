package io.github.architectplatform.engine.core.auth

import io.github.architectplatform.engine.core.auth.dto.AuthResponse
import io.github.architectplatform.engine.core.auth.dto.AuthStatusResponse
import io.github.architectplatform.engine.core.auth.dto.LoginRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory

/**
 * REST API controller for authentication operations.
 * 
 * Provides simple, provider-agnostic endpoints for authentication.
 * Providers (e.g., GitHub, GitLab) are registered by plugins and handle
 * their own authentication logic.
 */
@Controller("/auth")
class AuthController(
    private val loginProviderRegistry: LoginProviderRegistry
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Stores an authentication token for a provider.
     * 
     * @param provider The provider name (e.g., "github")
     * @param request The login request containing the token
     * @return Response indicating success or failure
     */
    @Post("/{provider}")
    fun login(
        @PathVariable provider: String,
        @Body request: LoginRequest
    ): AuthResponse {
        return try {
            val loginProvider = loginProviderRegistry.getProvider(provider)
            
            if (loginProvider == null) {
                return AuthResponse(
                    success = false,
                    message = "Provider '$provider' not found"
                )
            }
            
            if (request.token.isBlank()) {
                return AuthResponse(
                    success = false,
                    message = "Token cannot be empty"
                )
            }
            
            loginProvider.storeToken(request.token)
            logger.info("Authentication configured for provider: $provider")
            
            AuthResponse(
                success = true,
                message = "Token stored successfully for $provider"
            )
        } catch (e: Exception) {
            logger.error("Failed to store token for $provider: ${e.message}")
            AuthResponse(
                success = false,
                message = "Failed to store token: ${e.message}"
            )
        }
    }
    
    /**
     * Checks the authentication status for a provider.
     * 
     * @param provider The provider name
     * @return Status indicating if authenticated
     */
    @Get("/{provider}/status")
    fun getStatus(@PathVariable provider: String): AuthStatusResponse {
        val loginProvider = loginProviderRegistry.getProvider(provider)
        
        if (loginProvider == null) {
            return AuthStatusResponse(
                authenticated = false,
                provider = provider
            )
        }
        
        return AuthStatusResponse(
            authenticated = loginProvider.isAuthenticated(),
            provider = provider
        )
    }
    
    /**
     * Removes the stored token for a provider.
     * 
     * @param provider The provider name
     * @return Response indicating success or failure
     */
    @Delete("/{provider}")
    fun logout(@PathVariable provider: String): AuthResponse {
        return try {
            val loginProvider = loginProviderRegistry.getProvider(provider)
            
            if (loginProvider == null) {
                return AuthResponse(
                    success = false,
                    message = "Provider '$provider' not found"
                )
            }
            
            loginProvider.clearToken()
            logger.info("Authentication cleared for provider: $provider")
            
            AuthResponse(
                success = true,
                message = "Token removed successfully for $provider"
            )
        } catch (e: Exception) {
            logger.error("Failed to clear token for $provider: ${e.message}")
            AuthResponse(
                success = false,
                message = "Failed to clear token: ${e.message}"
            )
        }
    }
    
    /**
     * Lists all available authentication providers.
     * 
     * @return List of provider names
     */
    @Get("/providers")
    fun listProviders(): Map<String, Boolean> {
        return loginProviderRegistry.getAllProviders()
            .mapValues { it.value.isAuthenticated() }
    }
}
