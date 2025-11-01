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
}
