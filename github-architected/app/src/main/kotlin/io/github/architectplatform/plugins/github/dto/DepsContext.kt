package io.github.architectplatform.plugins.github.dto

data class DepsContext(
    val enabled: Boolean = true,
    val type: String = "renovate",
    val format: String = "json",
)
