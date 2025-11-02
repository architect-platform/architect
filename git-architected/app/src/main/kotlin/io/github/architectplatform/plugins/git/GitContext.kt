package io.github.architectplatform.plugins.git

/**
 * Configuration context for the Git plugin.
 *
 * Contains all settings for Git configuration and command proxying.
 *
 * @property config Map of Git configuration key-value pairs (e.g., user.name, user.email)
 * @property enabled Whether the Git plugin is enabled
 */
data class GitContext(
    val config: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)
