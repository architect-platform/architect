package io.github.architectplatform.api.core.tasks.phase

/**
 * Type alias for phase identifiers.
 */
typealias PhaseId = String

/**
 * Represents a stage in the project lifecycle.
 *
 * Phases organize tasks into logical groups and define execution order through dependencies.
 * Tasks associated with a phase inherit the phase's dependencies and are executed in the
 * order defined by the phase hierarchy.
 *
 * Example usage:
 * ```kotlin
 * enum class MyPhases : Phase {
 *   INIT {
 *     override val id = "init"
 *     override fun description() = "Initialize project"
 *     override fun parent() = null
 *     override fun depends() = emptyList()
 *   },
 *   BUILD {
 *     override val id = "build"
 *     override fun description() = "Build project"
 *     override fun parent() = null
 *     override fun depends() = listOf("init")
 *   }
 * }
 * ```
 */
interface Phase {
  /**
   * Unique identifier for this phase.
   */
  val id: PhaseId

  /**
   * Returns a human-readable description of what this phase represents.
   *
   * @return A description of the phase's purpose
   */
  fun description(): String = "No description provided for phase $id"

  /**
   * Returns the parent phase, if any.
   *
   * Parent phases are used to create phase hierarchies where a phase can be a sub-phase
   * of a more general phase.
   *
   * @return The parent phase, or null if this is a top-level phase
   */
  fun parent(): Phase?

  /**
   * Returns a list of phase IDs that this phase depends on.
   *
   * Dependencies ensure that tasks in the dependent phases are executed before tasks
   * in this phase.
   *
   * @return List of phase IDs that must complete before this phase
   */
  fun depends(): List<PhaseId> = emptyList()
}
