package io.github.architectplatform.engine.core.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.inject.Singleton
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import org.slf4j.LoggerFactory

/**
 * Manages authentication configuration for the Architect Engine.
 * 
 * Stores GitHub tokens and other credentials securely in the user's home directory.
 * Tokens are base64 encoded (basic obfuscation) and stored with restricted file permissions.
 */
@Singleton
class AuthConfigManager {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    
    private val configDir = Paths.get(System.getProperty("user.home"), ".architect-engine")
    private val configFile = configDir.resolve("config.yml").toFile()
    
    init {
        // Ensure config directory exists
        Files.createDirectories(configDir)
        
        // Set restrictive permissions on config directory if on Unix-like system
        try {
            if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
                Files.setPosixFilePermissions(configDir, PosixFilePermissions.fromString("rwx------"))
            }
        } catch (e: Exception) {
            logger.debug("Could not set POSIX permissions on config directory: ${e.message}")
        }
    }
    
    /**
     * Stores a GitHub token securely.
     * 
     * @param token The GitHub personal access token
     */
    fun setGitHubToken(token: String) {
        val config = loadConfig().toMutableMap()
        
        // Basic obfuscation using base64 encoding
        val encoded = Base64.getEncoder().encodeToString(token.toByteArray())
        config["github_token"] = encoded
        
        saveConfig(config)
        logger.info("GitHub token stored successfully")
    }
    
    /**
     * Retrieves the stored GitHub token.
     * 
     * @return The decoded GitHub token, or null if not set
     */
    fun getGitHubToken(): String? {
        val config = loadConfig()
        val encoded = config["github_token"] as? String ?: return null
        
        return try {
            String(Base64.getDecoder().decode(encoded))
        } catch (e: Exception) {
            logger.error("Failed to decode GitHub token")
            null
        }
    }
    
    /**
     * Checks if a GitHub token is configured.
     * 
     * @return true if a token exists, false otherwise
     */
    fun hasGitHubToken(): Boolean {
        return getGitHubToken() != null
    }
    
    /**
     * Removes the stored GitHub token.
     */
    fun clearGitHubToken() {
        val config = loadConfig().toMutableMap()
        config.remove("github_token")
        saveConfig(config)
        logger.info("GitHub token removed")
    }
    
    private fun loadConfig(): Map<String, Any> {
        if (!configFile.exists()) {
            return emptyMap()
        }
        
        return try {
            objectMapper.readValue(configFile)
        } catch (e: Exception) {
            logger.warn("Failed to load config file: ${e.message}")
            emptyMap()
        }
    }
    
    private fun saveConfig(config: Map<String, Any>) {
        try {
            objectMapper.writeValue(configFile, config)
            
            // Set restrictive permissions on config file if on Unix-like system
            if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
                Files.setPosixFilePermissions(configFile.toPath(), PosixFilePermissions.fromString("rw-------"))
            }
        } catch (e: Exception) {
            logger.error("Failed to save config file: ${e.message}", e)
            throw RuntimeException("Failed to save authentication config", e)
        }
    }
}
