package io.github.architectplatform.plugins.docs.dto

/**
 * Configuration context for the Docs plugin.
 *
 * Contains all settings for documentation management, building, and publishing.
 *
 * @property build Configuration for building documentation
 * @property publish Configuration for publishing documentation
 */
data class DocsContext(
    val build: BuildContext = BuildContext(),
    val publish: PublishContext = PublishContext()
)
