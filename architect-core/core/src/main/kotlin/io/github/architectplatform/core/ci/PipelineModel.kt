package io.github.architectplatform.core.ci

/**
 * Represents a single step within a CI job.
 */
data class CiStep(
  val name: String,
  val run: String? = null,
  val uses: String? = null,
  val with: Map<String, String> = emptyMap(),
)

/**
 * Represents a CI job (a unit of work in the pipeline).
 */
data class CiJob(
  val id: String,
  val name: String,
  val phase: String?,
  val needs: List<String>,
  val steps: List<CiStep>,
  val runsOn: String = "ubuntu-latest",
  val env: Map<String, String> = emptyMap(),
)

/**
 * Represents a branch/tag filter for CI triggers.
 */
data class CiBranchFilter(val branches: List<String>)

/**
 * Represents the trigger conditions for a CI pipeline.
 */
data class CiTrigger(
  val push: CiBranchFilter? = CiBranchFilter(listOf("main")),
  val pullRequest: CiBranchFilter? = CiBranchFilter(listOf("main")),
  val workflowDispatch: Boolean = true,
)

/**
 * Represents a full CI/CD pipeline definition.
 */
data class CiPipeline(
  val name: String,
  val on: CiTrigger,
  val jobs: List<CiJob>,
  val provider: String = "github-actions",
)
