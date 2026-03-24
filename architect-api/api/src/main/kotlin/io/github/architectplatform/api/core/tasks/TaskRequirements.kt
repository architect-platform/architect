package io.github.architectplatform.api.core.tasks

/**
 * Declares the runtime requirements that a task needs before it can execute.
 *
 * When a task declares requirements, Architect can check them prior to execution
 * and produce a precise, actionable error message instead of a cryptic failure.
 *
 * Example:
 * ```kotlin
 * SimpleTask(
 *   id = "docker-build",
 *   requires = TaskRequirements(
 *     tools = listOf("docker"),
 *     minToolVersions = mapOf("docker" to "20.0.0"),
 *     env = listOf("DOCKER_REGISTRY"),
 *     platform = setOf(Platform.LINUX, Platform.DARWIN),
 *   ),
 *   task = ::buildImage,
 * )
 * ```
 *
 * @property tools Names of CLI tools that must be available on PATH (e.g. `["docker", "git"]`)
 * @property minToolVersions Minimum required version per tool (tool name → version string)
 * @property env Environment variable names that must be set and non-empty
 * @property platform Set of platforms on which this task may run; empty means all platforms
 */
data class TaskRequirements(
  val tools: List<String> = emptyList(),
  val minToolVersions: Map<String, String> = emptyMap(),
  val env: List<String> = emptyList(),
  val platform: Set<Platform> = emptySet(),
)

/**
 * Operating-system platform identifiers used in [TaskRequirements.platform].
 */
enum class Platform {
  LINUX,
  DARWIN,
  WINDOWS,
  ;

  companion object {
    /** Detects the current host platform from the `os.name` system property. */
    fun current(): Platform {
      val osName = System.getProperty("os.name").lowercase()
      return when {
        osName.contains("mac") || osName.contains("darwin") -> DARWIN
        osName.contains("win") -> WINDOWS
        else -> LINUX
      }
    }
  }
}
