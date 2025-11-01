package io.github.architectplatform.plugins.github.dto

data class GithubContext(
    val release: GithubReleaseContext = GithubReleaseContext(),
    val pipelines: List<PipelineContext> = emptyList(),
    val deps: DepsContext = DepsContext(),
)
