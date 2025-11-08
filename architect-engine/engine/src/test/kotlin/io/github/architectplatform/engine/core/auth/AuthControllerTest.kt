package io.github.architectplatform.engine.core.auth

import io.github.architectplatform.engine.core.auth.dto.SetGitHubTokenRequest
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Integration tests for AuthController.
 */
@MicronautTest
class AuthControllerTest {
    
    @Inject
    @Client("/")
    lateinit var client: HttpClient
    
    @Inject
    lateinit var authConfigManager: AuthConfigManager
    
    @AfterEach
    fun cleanup() {
        authConfigManager.clearGitHubToken()
    }
    
    @Test
    fun `should set GitHub token via API`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        val request = HttpRequest.POST("/auth/github", SetGitHubTokenRequest(token))
        
        // When
        val response = client.toBlocking().retrieve(request)
        
        // Then
        assertNotNull(response)
        assertTrue(authConfigManager.hasGitHubToken())
        assertEquals(token, authConfigManager.getGitHubToken())
    }
    
    @Test
    fun `should check authentication status via API`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        authConfigManager.setGitHubToken(token)
        
        // When
        val response = client.toBlocking().retrieve("/auth/github/status")
        
        // Then
        assertNotNull(response)
        assertTrue(response.contains("\"authenticated\":true"))
    }
    
    @Test
    fun `should report not authenticated when no token stored`() {
        // Given - no token stored
        authConfigManager.clearGitHubToken()
        
        // When
        val response = client.toBlocking().retrieve("/auth/github/status")
        
        // Then
        assertNotNull(response)
        assertTrue(response.contains("\"authenticated\":false"))
    }
    
    @Test
    fun `should clear GitHub token via API`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        authConfigManager.setGitHubToken(token)
        assertTrue(authConfigManager.hasGitHubToken())
        
        // When
        val request = HttpRequest.DELETE<Any>("/auth/github")
        val response = client.toBlocking().retrieve(request)
        
        // Then
        assertNotNull(response)
        assertFalse(authConfigManager.hasGitHubToken())
    }
    
    @Test
    fun `should reject empty token`() {
        // Given
        val request = HttpRequest.POST("/auth/github", SetGitHubTokenRequest("   "))
        
        // When
        val response = client.toBlocking().retrieve(request)
        
        // Then
        assertNotNull(response)
        assertTrue(response.contains("\"success\":false"))
        assertFalse(authConfigManager.hasGitHubToken())
    }
}
