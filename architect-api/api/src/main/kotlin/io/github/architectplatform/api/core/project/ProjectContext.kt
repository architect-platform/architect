package io.github.architectplatform.api.core.project

import java.nio.file.Path

/**
 * Provides contextual services and configuration for task execution.
 *
 * ProjectContext encapsulates project-specific information including the project directory
 * and configuration data. This context is passed to tasks during execution.
 *
 * @property dir The root directory of the project
 * @property config Configuration data as a key-value map
 */
data class ProjectContext(val dir: Path, val config: Config)
