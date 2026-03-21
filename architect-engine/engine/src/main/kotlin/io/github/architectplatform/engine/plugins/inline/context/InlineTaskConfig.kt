package io.github.architectplatform.engine.plugins.inline.context

/**
 * Configuration for a single inline task defined in architect.yml under the `tasks:` key.
 *
 * Example:
 * ```yaml
 * tasks:
 *   deploy:
 *     description: "Deploy to staging"
 *     phase: PUBLISH
 *     depends: [build, test]
 *     run: "kubectl apply -f k8s/"
 * ```
 */
data class InlineTaskConfig(
    val description: String = "",
    val run: String = "",
    val phase: String? = null,
    val depends: List<String> = emptyList(),
)
