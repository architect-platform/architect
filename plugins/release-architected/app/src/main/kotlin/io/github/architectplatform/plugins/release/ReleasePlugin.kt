package io.github.architectplatform.plugins.release

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class ReleasePlugin : ArchitectPlugin<ReleaseContext> {
  override val id: String = "release-plugin"
  override val contextKey: String = "release"
  override val ctxClass: Class<ReleaseContext> = ReleaseContext::class.java
  override var context: ReleaseContext = ReleaseContext()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "additionalProperties" to false,
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean", "default" to true),
      "workingDirectory" to mapOf("type" to "string", "default" to "."),
      "strategy" to mapOf(
        "type" to "string",
        "enum" to listOf("semantic", "calendar", "manual"),
        "default" to "semantic",
      ),
      "changelog" to mapOf("type" to "boolean", "default" to true),
      "changelogPath" to mapOf("type" to "string", "default" to "CHANGELOG.md"),
      "releaseNotesPath" to mapOf("type" to "string", "default" to "build/release-notes.md"),
      "versionFiles" to mapOf(
        "type" to "array",
        "items" to mapOf("type" to "string"),
        "default" to listOf("VERSION"),
      ),
      "tagPrefix" to mapOf("type" to "string", "default" to "v"),
      "currentVersion" to mapOf("type" to "string"),
      "manualVersion" to mapOf("type" to "string"),
      "artifacts" to mapOf(
        "type" to "array",
        "default" to emptyList<Map<String, Any>>(),
        "items" to mapOf(
          "type" to "object",
          "required" to listOf("type"),
          "additionalProperties" to false,
          "properties" to mapOf(
            "type" to mapOf(
              "type" to "string",
              "enum" to listOf("npm", "docker", "github-release"),
            ),
            "enabled" to mapOf("type" to "boolean", "default" to true),
            "registry" to mapOf("type" to "string"),
            "directory" to mapOf("type" to "string"),
            "image" to mapOf("type" to "string"),
            "context" to mapOf("type" to "string"),
            "dockerfile" to mapOf("type" to "string"),
            "assets" to mapOf(
              "type" to "array",
              "items" to mapOf("type" to "string"),
              "default" to emptyList<String>(),
            ),
            "notesPath" to mapOf("type" to "string"),
            "tags" to mapOf(
              "type" to "array",
              "items" to mapOf("type" to "string"),
              "default" to emptyList<String>(),
            ),
          ),
        ),
      ),
      "publish" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "enabled" to mapOf("type" to "boolean", "default" to true),
          "beforeCommands" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
            "default" to emptyList<String>(),
          ),
          "afterCommands" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
            "default" to emptyList<String>(),
          ),
        ),
      ),
      "rollback" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "enabled" to mapOf("type" to "boolean", "default" to true),
          "deleteTag" to mapOf("type" to "boolean", "default" to true),
          "restoreVersionFiles" to mapOf("type" to "boolean", "default" to true),
          "restoreChangelog" to mapOf("type" to "boolean", "default" to true),
          "beforeCommands" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
            "default" to emptyList<String>(),
          ),
          "afterCommands" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
            "default" to emptyList<String>(),
          ),
        ),
      ),
    ),
  )

  override fun register(registry: TaskRegistry) {
    registry.add(ReleaseTask(ReleaseTask.Operation.PREPARE, CoreWorkflow.RELEASE, context))
    registry.add(ReleaseTask(ReleaseTask.Operation.PUBLISH, CoreWorkflow.PUBLISH, context))
    registry.add(ReleaseTask(ReleaseTask.Operation.ROLLBACK, CoreWorkflow.RELEASE, context))
  }
}
