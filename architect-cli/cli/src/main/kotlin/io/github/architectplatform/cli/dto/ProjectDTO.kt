package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Data transfer object representing a project registered with the Architect Engine.
 *
 * @property name The name of the project
 * @property path The file system path to the project root
 * @property context The project's execution context containing directory and configuration
 */
@Serdeable
data class ProjectDTO(
    val name: String,
    val path: String,
    val context: ProjectContextDTO,
) {
  /**
   * Execution context for a project.
   *
   * @property dir The working directory for task execution
   * @property config Configuration map containing project settings
   */
  @Serdeable
  data class ProjectContextDTO(
      val dir: String,
      val config: Map<String, Any>,
  )
}
