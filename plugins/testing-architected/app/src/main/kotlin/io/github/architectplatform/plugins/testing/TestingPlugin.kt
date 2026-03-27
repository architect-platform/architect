package io.github.architectplatform.plugins.testing

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class TestingPlugin : ArchitectPlugin<TestingContext> {
  override val id = "testing-plugin"
  override val contextKey: String = "testing"
  override val ctxClass: Class<TestingContext> = TestingContext::class.java
  override var context: TestingContext = TestingContext()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "additionalProperties" to false,
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean", "default" to true),
      "framework" to mapOf(
        "type" to "string",
        "enum" to listOf("auto", "junit", "junit-gradle", "pytest", "jest", "vitest", "go", "go-test", "cargo", "cargo-test"),
        "default" to "auto",
      ),
      "workingDirectory" to mapOf("type" to "string", "default" to "."),
      "parallel" to mapOf("type" to "boolean", "default" to true),
      "retryFlaky" to mapOf("type" to "integer", "default" to 2, "minimum" to 0),
      "commands" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "unit" to mapOf("type" to "string"),
          "integration" to mapOf("type" to "string"),
          "e2e" to mapOf("type" to "string"),
          "coverage" to mapOf("type" to "string"),
        ),
      ),
      "coverage" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "enabled" to mapOf("type" to "boolean", "default" to true),
          "threshold" to mapOf("type" to "integer", "default" to 80, "minimum" to 0, "maximum" to 100),
          "reporter" to mapOf(
            "type" to "array",
            "items" to mapOf(
              "type" to "string",
              "enum" to listOf("html", "lcov", "cobertura", "xml", "text", "text-summary"),
            ),
            "default" to listOf("html", "lcov", "cobertura"),
          ),
          "reportPaths" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
            "default" to emptyList<String>(),
          ),
        ),
      ),
    ),
  )

  override fun register(registry: TaskRegistry) {
    registry.add(TestingTask(TestingTask.Suite.UNIT, CoreWorkflow.TEST, context))
    registry.add(TestingTask(TestingTask.Suite.INTEGRATION, CoreWorkflow.TEST, context))
    registry.add(TestingTask(TestingTask.Suite.E2E, CoreWorkflow.TEST, context))
    registry.add(TestingTask(TestingTask.Suite.COVERAGE, CoreWorkflow.VERIFY, context))
  }
}
