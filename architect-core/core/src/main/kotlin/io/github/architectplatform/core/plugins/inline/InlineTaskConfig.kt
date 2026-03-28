package io.github.architectplatform.core.plugins.inline

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration for a single inline task defined in architect.yml under the `tasks:` key.
 *
 * Example:
 * ```yaml
 * tasks:
 *   docker-build:
 *     description: "Build the Docker image"
 *     phase: BUILD
 *     depends: [test]
 *     run: "docker build -t myapp ."
 *     requires:
 *       tools: [docker]
 *       min-tool-versions:
 *         docker: "20.0.0"
 *       env: [DOCKER_REGISTRY]
 *       platform: [linux, darwin]
 *     condition: "env.CI == 'true'"
 *     timeout: 300s
 *     onFailure: RETRY
 *     retryAttempts: 3
 * ```
 */
data class InlineTaskConfig(
    val description: String = "",
    val run: String = "",
    val phase: String? = null,
    val depends: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val requires: InlineTaskRequirements? = null,
    val timeout: String? = null,
    val condition: String? = null,
    val onFailure: String? = null,
    val retryAttempts: Int? = null,
)

/**
 * Inline YAML representation of task runtime requirements.
 * Maps to [io.github.architectplatform.api.core.tasks.TaskRequirements] after resolution.
 */
data class InlineTaskRequirements(
    val tools: List<String> = emptyList(),
    @JsonProperty("min-tool-versions")
    val minToolVersions: Map<String, String> = emptyMap(),
    val env: List<String> = emptyList(),
    val platform: List<String> = emptyList(),
)
