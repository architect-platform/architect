package io.github.architectplatform.engine.core.auth

import jakarta.inject.Singleton

/**
 * GitHub-specific implementation of LoginProvider.
 * 
 * This is a built-in provider for GitHub authentication. Plugins can create
 * their own providers by implementing the LoginProvider interface.
 */
@Singleton
class GitHubLoginProvider(
    private val authConfigManager: AuthConfigManager
) : LoginProvider {
    
    override fun getProviderName(): String = "github"
    
    override fun storeToken(token: String) {
        authConfigManager.setGitHubToken(token)
    }
    
    override fun getToken(): String? {
        return authConfigManager.getGitHubToken()
    }
    
    override fun isAuthenticated(): Boolean {
        return authConfigManager.hasGitHubToken()
    }
    
    override fun clearToken() {
        authConfigManager.clearGitHubToken()
    }
}
