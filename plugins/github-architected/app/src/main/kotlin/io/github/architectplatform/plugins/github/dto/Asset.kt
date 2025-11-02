package io.github.architectplatform.plugins.github.dto

/**
 * Represents an asset to be uploaded with a GitHub release.
 *
 * @property name The display name of the asset in the release
 * @property path The file system path to the asset file
 */
data class Asset(
    val name: String,
    val path: String,
)
