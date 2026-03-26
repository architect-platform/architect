package io.github.architectplatform.api.core.tasks

import io.github.architectplatform.api.core.project.ProjectContext

/**
 * Provides contextual information for task execution.
 *
 * TaskContext is a lightweight interface that provides access to the project context
 * during task execution. It can be extended to provide additional context-specific data.
 */
interface TaskContext {
  /**
   * The project context containing project directory and configuration.
   */
  val projectContext: ProjectContext

  /**
   * Data produced by upstream tasks in the dependency chain.
   *
   * Keys are task IDs; values are the [TaskResult.data] maps from those tasks.
   * Only tasks that are direct or transitive dependencies of the current task
   * appear here. Empty if no upstream tasks produced data.
   *
   * Example usage:
   * ```kotlin
   * val buildOutput = upstreamData["gradle-build"]?.get("artifactPath") as? String
   * ```
   */
  val upstreamData: Map<String, Map<String, Any>>
    get() = emptyMap()
}
