package io.github.architectplatform.engine.core.auth

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for managing authentication providers.
 * 
 * Plugins can register their own LoginProvider implementations to enable
 * authentication for different services.
 */
@Singleton
class LoginProviderRegistry {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val providers = ConcurrentHashMap<String, LoginProvider>()
    
    /**
     * Registers a login provider.
     * 
     * @param provider The login provider to register
     */
    fun registerProvider(provider: LoginProvider) {
        val providerName = provider.getProviderName()
        providers[providerName] = provider
        logger.info("Registered login provider: $providerName")
    }
    
    /**
     * Gets a login provider by name.
     * 
     * @param providerName The name of the provider
     * @return The login provider, or null if not found
     */
    fun getProvider(providerName: String): LoginProvider? {
        return providers[providerName]
    }
    
    /**
     * Gets all registered providers.
     * 
     * @return Map of provider names to providers
     */
    fun getAllProviders(): Map<String, LoginProvider> {
        return providers.toMap()
    }
    
    /**
     * Checks if a provider is registered.
     * 
     * @param providerName The name of the provider
     * @return true if registered, false otherwise
     */
    fun hasProvider(providerName: String): Boolean {
        return providers.containsKey(providerName)
    }
}
