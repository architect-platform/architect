package io.github.architectplatform.plugins.javascript

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

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "additionalProperties" to false,
    "properties" to mapOf(
      "packageManager" to mapOf(
        "type" to "string",
        "enum" to listOf("npm", "yarn", "pnpm", "bun"),
        "default" to "npm",
      ),
      "yarnMode" to mapOf(
        "type" to "string",
        "enum" to listOf("auto", "classic", "berry"),
        "default" to "auto",
      ),
      "workingDirectory" to mapOf(
        "type" to "string",
        "default" to ".",
      ),
      "publishAccess" to mapOf(
        "type" to "string",
        "enum" to listOf("public", "restricted"),
        "default" to "public",
      ),
      "defaultVersionBump" to mapOf(
        "type" to "string",
        "enum" to listOf("major", "minor", "patch", "prerelease"),
        "default" to "patch",
      ),
    ),
  )

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
    registry.add(JavaScriptTask("workspace-check", CodeWorkflow.VERIFY, context))
    registry.add(JavaScriptTask("lockfile-check", CodeWorkflow.VERIFY, context))
    registry.add(JavaScriptTask("audit", CodeWorkflow.VERIFY, context))
    registry.add(JavaScriptTask("build", CodeWorkflow.BUILD, context))
    registry.add(JavaScriptTask("test", CodeWorkflow.TEST, context))
    registry.add(JavaScriptTask("lint", CodeWorkflow.TEST, context))
    registry.add(JavaScriptTask("dev", CodeWorkflow.RUN, context))
    registry.add(JavaScriptTask("version", CodeWorkflow.RELEASE, context))
    registry.add(JavaScriptTask("publish", CodeWorkflow.PUBLISH, context))
  }
}
