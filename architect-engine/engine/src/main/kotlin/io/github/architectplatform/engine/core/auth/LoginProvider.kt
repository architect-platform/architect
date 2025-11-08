package io.github.architectplatform.engine.core.auth

/**
 * Interface for authentication providers.
 * 
 * Plugins can implement this interface to provide custom authentication flows
 * for different services (GitHub, GitLab, Bitbucket, etc.)
 */
interface LoginProvider {
    
    /**
     * Returns the name of the provider (e.g., "github", "gitlab").
     */
    fun getProviderName(): String
    
    /**
     * Stores an authentication token for this provider.
     * 
     * @param token The authentication token to store
     */
    fun storeToken(token: String)
    
    /**
     * Retrieves the stored authentication token for this provider.
     * 
     * @return The stored token, or null if not authenticated
     */
    fun getToken(): String?
    
    /**
     * Checks if the user is authenticated with this provider.
     * 
     * @return true if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean
    
    /**
     * Clears the stored authentication token.
     */
    fun clearToken()
}
