package io.github.architectplatform.cli.dto

data class ProjectHealthDTO(
  val name: String,
  val path: String,
  val valid: Boolean,
  val errors: List<String>,
  val warnings: List<String>,
  val taskCount: Int,
  val lastBuildSuccess: Boolean?,
  val lastBuildAgeSeconds: Long?,
)

data class MonorepoHealthDTO(
  val rootProject: String,
  val projects: List<ProjectHealthDTO>,
  val healthyCount: Int,
  val unhealthyCount: Int,
  val timestamp: String,
) {
  val totalProjects: Int get() = projects.size
  val healthy: Boolean get() = unhealthyCount == 0
}
