package io.github.architectplatform.plugins.docs.dto

/**
 * Configuration context for documentation publishing.
 *
 * This class defines the configuration for publishing documentation to various targets.
 *
 * @property enabled Whether documentation publishing is enabled
 * @property githubPages Whether to publish to GitHub Pages
 * @property branch Branch to publish documentation to (for GitHub Pages)
 * @property domain Custom domain for GitHub Pages
 * @property cname Whether to generate CNAME file for custom domain
 */
data class PublishContext(
    val enabled: Boolean = true,
    val githubPages: Boolean = true,
    val branch: String = "gh-pages",
    val domain: String = "",
    val cname: Boolean = true
)
