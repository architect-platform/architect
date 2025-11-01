package io.github.architectplatform.plugins.github.dto

data class PipelineContext(
    val name: String,
    val type: String,
    val path: String = "**",
    val branch: String = "main",
)
