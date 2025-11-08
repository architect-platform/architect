package io.github.architectplatform.engine.core.auth

import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton

/**
 * Configuration for authentication system.
 * 
 * Registers built-in login providers on application startup.
 */
@Singleton
class AuthConfiguration(
    private val loginProviderRegistry: LoginProviderRegistry,
    private val gitHubLoginProvider: GitHubLoginProvider
) : ApplicationEventListener<StartupEvent> {
    
    override fun onApplicationEvent(event: StartupEvent) {
        // Register built-in GitHub provider
        loginProviderRegistry.registerProvider(gitHubLoginProvider)
    }
}
