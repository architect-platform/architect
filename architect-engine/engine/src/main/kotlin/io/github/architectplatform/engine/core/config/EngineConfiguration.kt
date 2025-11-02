package io.github.architectplatform.engine.core.config

/**
 * Configuration constants for the Architect Engine.
 * 
 * This object centralizes all configuration property names and default values
 * used throughout the engine, making them easy to discover and modify.
 */
object EngineConfiguration {
    
    /**
     * Cache configuration properties
     */
    object Cache {
        const val ENABLED = "architect.cache.enabled"
        const val DEFAULT_ENABLED = false
    }
    
    /**
     * Project configuration properties
     */
    object Project {
        const val CACHE_ENABLED = "architect.engine.core.project.cache.enabled"
        const val DEFAULT_CACHE_ENABLED = true
    }
    
    /**
     * Event collector configuration properties
     */
    object EventCollector {
        const val REPLAY_SIZE = "architect.engine.events.replay-size"
        const val DEFAULT_REPLAY_SIZE = 64
        
        const val BUFFER_CAPACITY = "architect.engine.events.buffer-capacity"
        const val DEFAULT_BUFFER_CAPACITY = 64
    }
    
    /**
     * Command executor configuration properties
     */
    object CommandExecutor {
        const val TIMEOUT_SECONDS = "architect.engine.executor.timeout-seconds"
        const val DEFAULT_TIMEOUT_SECONDS = 300L
        
        const val REDIRECT_ERROR_STREAM = "architect.engine.executor.redirect-error-stream"
        const val DEFAULT_REDIRECT_ERROR_STREAM = true
    }
    
    /**
     * Plugin loader configuration properties
     */
    object PluginLoader {
        const val DOWNLOAD_TIMEOUT_SECONDS = "architect.engine.plugins.download-timeout-seconds"
        const val DEFAULT_DOWNLOAD_TIMEOUT_SECONDS = 300L
        
        const val DOWNLOAD_RETRY_ATTEMPTS = "architect.engine.plugins.download-retry-attempts"
        const val DEFAULT_DOWNLOAD_RETRY_ATTEMPTS = 3
        
        const val USER_AGENT = "architect.engine.plugins.user-agent"
        const val DEFAULT_USER_AGENT = "ArchitectPlatform/1.0"
    }
}
