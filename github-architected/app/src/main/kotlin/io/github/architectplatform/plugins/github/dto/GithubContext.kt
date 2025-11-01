package io.github.architectplatform.plugins.github.dto

/**
 * Configuration context for the GitHub plugin.
 *
 * Contains all settings for GitHub-related automation including
 * release management, CI/CD pipelines, and dependency management.
 *
 * @property release Configuration for automated releases
 * @property pipelines List of GitHub Actions pipeline configurations
 * @property deps Configuration for dependency management tools
 */
data class GithubContext(
    val release: GithubReleaseContext = GithubReleaseContext(),
    val pipelines: List<PipelineContext> = emptyList(),
    val deps: DepsContext = DepsContext(),
)
