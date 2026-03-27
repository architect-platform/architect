package io.github.architectplatform.plugins.security

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class SecurityPlugin : ArchitectPlugin<SecurityContext> {
  override val id = "security-plugin"
  override val contextKey: String = "security"
  override val ctxClass: Class<SecurityContext> = SecurityContext::class.java
  override var context: SecurityContext = SecurityContext()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "additionalProperties" to false,
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean", "default" to true),
      "workingDirectory" to mapOf("type" to "string", "default" to "."),
      "scan" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "enabled" to mapOf("type" to "boolean", "default" to true),
          "tools" to mapOf(
            "type" to "array",
            "items" to mapOf(
              "type" to "string",
              "enum" to listOf("trivy", "snyk", "codeql", "npm-audit", "pip-audit", "cargo-audit"),
            ),
            "default" to listOf("trivy"),
          ),
          "failOn" to mapOf(
            "type" to "string",
            "enum" to listOf("critical", "high", "medium", "low"),
            "default" to "critical",
          ),
          "codeqlDatabase" to mapOf("type" to "string"),
          "codeqlQuerySuite" to mapOf("type" to "string"),
          "codeqlOutput" to mapOf("type" to "string", "default" to "build/reports/security/codeql.sarif"),
          "commands" to mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
              "trivy" to mapOf("type" to "string"),
              "snyk" to mapOf("type" to "string"),
              "codeql" to mapOf("type" to "string"),
            ),
          ),
        ),
      ),
      "audit" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "enabled" to mapOf("type" to "boolean", "default" to true),
          "tools" to mapOf(
            "type" to "array",
            "items" to mapOf(
              "type" to "string",
              "enum" to listOf("npm-audit", "pip-audit", "cargo-audit"),
            ),
            "default" to emptyList<String>(),
          ),
          "failOn" to mapOf(
            "type" to "string",
            "enum" to listOf("critical", "high", "medium", "low"),
            "default" to "high",
          ),
          "commands" to mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
              "npmAudit" to mapOf("type" to "string"),
              "pipAudit" to mapOf("type" to "string"),
              "cargoAudit" to mapOf("type" to "string"),
            ),
          ),
        ),
      ),
      "sbom" to mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
          "enabled" to mapOf("type" to "boolean", "default" to true),
          "format" to mapOf(
            "type" to "string",
            "enum" to listOf("cyclonedx", "spdx"),
            "default" to "cyclonedx",
          ),
          "output" to mapOf("type" to "string", "default" to "sbom.json"),
          "tool" to mapOf(
            "type" to "string",
            "enum" to listOf("trivy"),
            "default" to "trivy",
          ),
          "command" to mapOf("type" to "string"),
        ),
      ),
    ),
  )

  override fun register(registry: TaskRegistry) {
    registry.add(SecurityTask(SecurityTask.Operation.SCAN, CoreWorkflow.VERIFY, context))
    registry.add(SecurityTask(SecurityTask.Operation.AUDIT, CoreWorkflow.VERIFY, context))
    registry.add(SecurityTask(SecurityTask.Operation.SBOM, CoreWorkflow.VERIFY, context))
  }
}
