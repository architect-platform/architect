package io.github.architectplatform.plugins.github.dto

/**
 * Configuration for dependency management tools.
 *
 * Controls the initialization of automated dependency update tools
 * like Renovate or Dependabot.
 *
 * @property enabled Whether dependency management should be configured
 * @property type The type of dependency management tool (e.g., "renovate")
 * @property format The configuration file format (e.g., "json")
 */
data class DepsContext(
    val enabled: Boolean = true,
    val type: String = "renovate",
    val format: String = "json",
)
