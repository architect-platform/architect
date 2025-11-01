package io.github.architectplatform.plugins.github.dto

data class GithubReleaseContext(
    val enabled: Boolean = true,
    val message: String = "chore(release): \${nextRelease.version} [skip ci]",
    val assets: List<Asset> = listOf(),
    val git_assets: List<String> = emptyList(),
)
