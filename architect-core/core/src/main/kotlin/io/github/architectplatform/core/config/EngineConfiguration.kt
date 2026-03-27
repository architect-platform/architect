package io.github.architectplatform.core.config

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
        const val TTL_SECONDS = "architect.cache.ttl-seconds"
        const val DEFAULT_TTL_SECONDS = 0L
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

        /**
         * Shared-flow overflow handling strategy.
         * - BLOCK: suspend publishers until consumers catch up
         * - DROP_OLDEST: drop oldest buffered events
         * - EXPAND: increase buffer size and fallback to drop-oldest if still exceeded
         */
        const val OVERFLOW_STRATEGY = "architect.engine.events.overflow-strategy"
        const val DEFAULT_OVERFLOW_STRATEGY = "DROP_OLDEST"
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

    /**
     * Plugin security configuration properties.
     */
    object PluginSecurity {
        /**
         * When true, plugin permission declaration violations fail project loading/validation.
         * When false, violations are logged as warnings.
         */
        const val STRICT_MODE = "architect.engine.plugins.security.strict-mode"
        const val DEFAULT_STRICT_MODE = false
    }

    /**
     * Task execution configuration properties
     */
    object TaskExecution {
        /** When true, tasks with no ordering dependency between them execute concurrently. */
        const val PARALLEL_ENABLED = "architect.engine.executor.parallel-execution"
        const val DEFAULT_PARALLEL_ENABLED = true

        /**
         * Maximum number of tasks that may execute concurrently across all parallel batches.
         * 0 means unlimited (no semaphore). Applies per-engine-instance (not per-execution).
         */
        const val MAX_CONCURRENT_TASKS = "architect.engine.executor.max-concurrent-tasks"
        const val DEFAULT_MAX_CONCURRENT_TASKS = 0

        /**
         * Wall-clock timeout for an entire execution in seconds.
         * 0 means unlimited. When exceeded, the execution is cancelled.
         */
        const val EXECUTION_TIMEOUT_SECONDS = "architect.engine.executor.execution-timeout-seconds"
        const val DEFAULT_EXECUTION_TIMEOUT_SECONDS = 0L
    }

    /**
     * Task retry configuration properties (used as engine-wide defaults for
     * FailureStrategy.RETRY when a task does not specify explicit backoff values).
     */
    object TaskRetry {
        const val DEFAULT_BACKOFF_MS = "architect.engine.executor.retry.backoff-ms"
        const val DEFAULT_BACKOFF_MS_VALUE = 100L

        const val DEFAULT_EXPONENTIAL = "architect.engine.executor.retry.exponential"
        const val DEFAULT_EXPONENTIAL_VALUE = true

        const val DEFAULT_JITTER = "architect.engine.executor.retry.jitter"
        const val DEFAULT_JITTER_VALUE = true
    }
}
