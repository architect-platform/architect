package io.github.architectplatform.plugins.github.dto

/**
 * Configuration for GitHub release automation.
 *
 * Controls semantic-release behavior including versioning,
 * changelog generation, and asset publishing.
 *
 * @property enabled Whether automatic releases are enabled
 * @property message Commit message template for release commits
 * @property assets List of files to attach to the GitHub release
 * @property git_assets List of file patterns to include in the release commit
 */
data class GithubReleaseContext(
    val enabled: Boolean = true,
    val message: String = "chore(release): \${nextRelease.version} [skip ci]",
    val assets: List<Asset> = listOf(),
    val git_assets: List<String> = emptyList(),
)
