package io.github.architectplatform.api.components.workflows.core

import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.tasks.phase.PhaseId

/**
 * Core phases in the standard project lifecycle.
 *
 * CoreWorkflow defines the standard phases that most projects go through, from
 * initialization to publishing. Each phase depends on the successful completion
 * of previous phases, creating a dependency chain.
 *
 * The standard lifecycle is:
 * 1. INIT - Initialize the project (setup, install dependencies)
 * 2. LINT - Lint the code (code style checking)
 * 3. VERIFY - Verify the code (type checking, validation)
 * 4. BUILD - Build the project (compile, package)
 * 5. RUN - Run the project (execute the application)
 * 6. TEST - Test the project (run test suites)
 * 7. RELEASE - Release the project (create release artifacts)
 * 8. PUBLISH - Publish the project (deploy to repositories)
 *
 * Example usage:
 * ```kotlin
 * val task = SimpleTask(
 *   id = "compile",
 *   description = "Compiles the source code",
 *   phase = CoreWorkflow.BUILD
 * ) { env, ctx ->
 *   // Task implementation
 *   TaskResult.success()
 * }
 * ```
 */
enum class CoreWorkflow(
    override val id: String,
    private val description: String,
    private val dependsOn: List<PhaseId> = emptyList(),
) : Phase {

  /** Initialize the project - setup, install dependencies, prepare environment */
  INIT("init", "Initialize the project"),
  
  /** Lint the project - check code style and formatting */
  LINT("lint", "Lint the project", listOf("init")),
  
  /** Verify the project - type checking, validation, static analysis */
  VERIFY("verify", "Verify the project", listOf("lint")),
  
  /** Build the project - compile, package, create artifacts */
  BUILD("build", "Build the project", listOf("verify")),
  
  /** Run the project - execute the application */
  RUN("run", "Run the project", listOf("build")),
  
  /** Test the project - run test suites */
  TEST("test", "Test the project", listOf("build")),
  
  /** Release the project - create release artifacts, tag versions */
  RELEASE("release", "Release the project", listOf("test")),
  
  /** Publish the project - deploy to repositories, registries */
  PUBLISH("publish", "Publish the project", listOf("release"));

  override fun description(): String = description

  override fun depends(): List<PhaseId> {
    return dependsOn
  }

  override fun parent(): Phase? {
    return null // CoreWorkflow does not have a parent phase
  }
}
