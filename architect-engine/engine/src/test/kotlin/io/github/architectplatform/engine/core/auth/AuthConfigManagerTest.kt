package io.github.architectplatform.engine.core.auth

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Unit tests for AuthConfigManager.
 */
@MicronautTest
class AuthConfigManagerTest {
    
    @Inject
    lateinit var authConfigManager: AuthConfigManager
    
    private val configFile = Paths.get(System.getProperty("user.home"), ".architect-engine", "config.yml")
    
    @AfterEach
    fun cleanup() {
        // Clean up after each test
        authConfigManager.clearGitHubToken()
    }
    
    @Test
    fun `should store and retrieve GitHub token`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        
        // When
        authConfigManager.setGitHubToken(token)
        
        // Then
        val retrievedToken = authConfigManager.getGitHubToken()
        assertEquals(token, retrievedToken)
    }
    
    @Test
    fun `should report token exists after storing`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        
        // When
        authConfigManager.setGitHubToken(token)
        
        // Then
        assertTrue(authConfigManager.hasGitHubToken())
    }
    
    @Test
    fun `should report no token when not stored`() {
        // Given/When - no token stored
        authConfigManager.clearGitHubToken()
        
        // Then
        assertFalse(authConfigManager.hasGitHubToken())
        assertNull(authConfigManager.getGitHubToken())
    }
    
    @Test
    fun `should clear stored token`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        authConfigManager.setGitHubToken(token)
        assertTrue(authConfigManager.hasGitHubToken())
        
        // When
        authConfigManager.clearGitHubToken()
        
        // Then
        assertFalse(authConfigManager.hasGitHubToken())
        assertNull(authConfigManager.getGitHubToken())
    }
    
    @Test
    fun `should create config file with restricted permissions on Unix`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        
        // When
        authConfigManager.setGitHubToken(token)
        
        // Then
        assertTrue(Files.exists(configFile))
        
        // Check permissions only on Unix-like systems
        if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            val permissions = Files.getPosixFilePermissions(configFile)
            // Should only have owner read/write permissions
            assertTrue(permissions.toString().contains("OWNER_READ"))
            assertTrue(permissions.toString().contains("OWNER_WRITE"))
            assertFalse(permissions.toString().contains("GROUP"))
            assertFalse(permissions.toString().contains("OTHERS"))
        }
    }
    
    @Test
    fun `should not expose token in plain text in config file`() {
        // Given
        val token = "ghp_test1234567890abcdefghijklmnopqrst"
        
        // When
        authConfigManager.setGitHubToken(token)
        
        // Then
        val configContent = Files.readString(configFile)
        // Token should be encoded, not in plain text
        assertFalse(configContent.contains(token), "Token should not be stored in plain text")
        // Should contain some form of encoded data
        assertTrue(configContent.contains("github_token:"), "Config should have github_token key")
    }
    
    @Test
    fun `should handle multiple token updates`() {
        // Given
        val token1 = "ghp_first1234567890abcdefghijklmnopqr"
        val token2 = "ghp_second1234567890abcdefghijklmnop"
        
        // When
        authConfigManager.setGitHubToken(token1)
        assertEquals(token1, authConfigManager.getGitHubToken())
        
        authConfigManager.setGitHubToken(token2)
        
        // Then
        assertEquals(token2, authConfigManager.getGitHubToken())
    }
}
