package io.github.architectplatform.engine.core.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service for GitHub authentication using OAuth Device Flow.
 * 
 * Implements GitHub's device flow authentication which is ideal for CLI applications.
 * https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps#device-flow
 */
@Singleton
class GitHubAuthService(
    private val httpClient: HttpClient,
    private val authConfigManager: AuthConfigManager
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    companion object {
        // GitHub OAuth App credentials for Architect CLI
        // These would normally be stored in configuration
        private const val CLIENT_ID = "Iv1.b507a08c87ecfe98" // Example client ID - would need to be registered
        private const val DEVICE_CODE_URL = "https://github.com/login/device/code"
        private const val ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val VERIFY_TOKEN_URL = "https://api.github.com/user"
    }
    
    /**
     * Initiates the device flow authentication.
     * 
     * @return DeviceCodeResponse containing the user code and verification URI
     */
    fun initiateDeviceFlow(): DeviceCodeResponse {
        logger.info("Initiating GitHub device flow authentication")
        
        val request = HttpRequest.POST(DEVICE_CODE_URL, mapOf(
            "client_id" to CLIENT_ID,
            "scope" to "repo" // Request repo access for private repositories
        ))
            .header("Accept", "application/json")
        
        val response = httpClient.toBlocking().retrieve(request, String::class.java)
        return objectMapper.readValue(response)
    }
    
    /**
     * Polls GitHub to check if the user has authorized the device.
     * 
     * @param deviceCode The device code from the initial request
     * @param interval Polling interval in seconds
     * @return The access token if successful, null if still pending
     */
    fun pollForAccessToken(deviceCode: String, interval: Int = 5): String? {
        logger.debug("Polling for access token")
        
        val request = HttpRequest.POST(ACCESS_TOKEN_URL, mapOf(
            "client_id" to CLIENT_ID,
            "device_code" to deviceCode,
            "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
        ))
            .header("Accept", "application/json")
        
        return try {
            val response = httpClient.toBlocking().retrieve(request, String::class.java)
            val tokenResponse: AccessTokenResponse = objectMapper.readValue(response)
            
            when {
                tokenResponse.access_token != null -> {
                    logger.info("Successfully obtained access token")
                    authConfigManager.setGitHubToken(tokenResponse.access_token)
                    tokenResponse.access_token
                }
                tokenResponse.error == "authorization_pending" -> {
                    logger.debug("Authorization still pending")
                    null
                }
                tokenResponse.error == "slow_down" -> {
                    logger.debug("Rate limited, need to slow down polling")
                    null
                }
                else -> {
                    logger.error("Failed to get access token: ${tokenResponse.error}")
                    throw IllegalStateException("Authentication failed: ${tokenResponse.error_description}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error polling for access token: ${e.message}")
            throw e
        }
    }
    
    /**
     * Verifies a GitHub token by making a request to the GitHub API.
     * 
     * @param token The GitHub token to verify
     * @return GitHubUser information if token is valid
     */
    fun verifyToken(token: String): GitHubUser {
        logger.debug("Verifying GitHub token")
        
        val request = HttpRequest.GET<Any>(VERIFY_TOKEN_URL)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
        
        return try {
            val response = httpClient.toBlocking().retrieve(request, String::class.java)
            val user: GitHubUser = objectMapper.readValue(response)
            logger.info("Token verified for user: ${user.login}")
            user
        } catch (e: Exception) {
            logger.error("Failed to verify token: ${e.message}")
            throw IllegalArgumentException("Invalid GitHub token", e)
        }
    }
}

/**
 * Response from GitHub's device code endpoint.
 */
data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

/**
 * Response from GitHub's access token endpoint.
 */
data class AccessTokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null,
    val error_uri: String? = null
)

/**
 * GitHub user information.
 */
data class GitHubUser(
    val login: String,
    val id: Long,
    val name: String?,
    val email: String?
)
