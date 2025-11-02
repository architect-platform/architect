package io.github.architectplatform.plugins.gradlearchitected

/**
 * Context containing all Gradle project configurations.
 *
 * @property projects List of Gradle projects to be managed by the plugin
 */
data class GradleContext(val projects: List<GradleProjectContext> = emptyList())
