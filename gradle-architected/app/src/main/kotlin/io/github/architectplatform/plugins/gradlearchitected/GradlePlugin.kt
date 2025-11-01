package io.github.architectplatform.plugins.gradlearchitected

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Architect plugin for Gradle build tool integration.
 *
 * Provides integration between Gradle build tasks and the Architect workflow system,
 * enabling execution of Gradle commands (build, test, run, publish) within
 * Architect's structured workflow phases.
 *
 * Supports:
 * - Multi-module Gradle projects
 * - Conditional task execution
 * - GitHub Packages publishing
 * - Custom Gradle wrapper locations
 */
class GradlePlugin : ArchitectPlugin<GradleContext> {
  override val id = "gradle-plugin"
  override val contextKey: String = "gradle"
  override val ctxClass: Class<GradleContext> = GradleContext::class.java
  override var context: GradleContext = GradleContext()

  /**
   * Registers Gradle tasks with the task registry.
   *
   * Registered tasks:
   * - gradle-: Init task (no command executed)
   * - gradle-build: Builds all configured Gradle projects
   * - gradle-test: Runs tests for all configured projects
   * - gradle-run: Runs the application
   * - gradle-publishGprPublicationToGitHubPackagesRepository: Publishes to GitHub Packages (conditional)
   *
   * @param registry The task registry to add tasks to
   */
  override fun register(registry: TaskRegistry) {
    // just "" because gradle tasks don't need a specific command to initialize
    registry.add(GradleTask("", CodeWorkflow.INIT, context))
    registry.add(GradleTask("build", CodeWorkflow.BUILD, context))
    registry.add(GradleTask("test", CodeWorkflow.TEST, context))
    registry.add(GradleTask("run", CodeWorkflow.RUN, context))
    registry.add(
        GradleTask(
            "publishGprPublicationToGitHubPackagesRepository", CodeWorkflow.PUBLISH, context) {
              it.githubPackageRelease
            })
  }
}
