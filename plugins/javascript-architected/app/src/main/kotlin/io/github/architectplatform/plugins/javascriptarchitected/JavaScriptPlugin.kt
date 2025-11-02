package io.github.architectplatform.plugins.javascriptarchitected

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Architect plugin for JavaScript/Node.js project integration.
 *
 * Provides integration between JavaScript package managers (npm, yarn, pnpm) and
 * the Architect workflow system, enabling execution of common JavaScript commands
 * within Architect's structured workflow phases.
 *
 * Supports:
 * - npm, yarn, and pnpm package managers
 * - Standard JavaScript workflows (install, build, test, lint, run)
 * - Custom working directories
 */
class JavaScriptPlugin : ArchitectPlugin<JavaScriptContext> {
  override val id = "javascript-plugin"
  override val contextKey: String = "javascript"
  override val ctxClass: Class<JavaScriptContext> = JavaScriptContext::class.java
  override var context: JavaScriptContext = JavaScriptContext()

  /**
   * Registers JavaScript tasks with the task registry.
   *
   * Registered tasks:
   * - javascript-install: Installs dependencies (npm install / yarn / pnpm)
   * - javascript-build: Builds the project (npm run build)
   * - javascript-test: Runs tests (npm test)
   * - javascript-lint: Runs linter (npm run lint)
   * - javascript-dev: Starts development server (npm run dev)
   *
   * @param registry The task registry to add tasks to
   */
  override fun register(registry: TaskRegistry) {
    registry.add(JavaScriptTask("install", CodeWorkflow.INIT, context))
    registry.add(JavaScriptTask("build", CodeWorkflow.BUILD, context))
    registry.add(JavaScriptTask("test", CodeWorkflow.TEST, context))
    registry.add(JavaScriptTask("lint", CodeWorkflow.TEST, context))
    registry.add(JavaScriptTask("dev", CodeWorkflow.RUN, context))
  }
}
