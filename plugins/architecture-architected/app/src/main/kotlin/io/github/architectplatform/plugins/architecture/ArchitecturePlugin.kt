package io.github.architectplatform.plugins.architecture

import io.github.architectplatform.api.components.workflows.code.CodeWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Architect plugin for architectural rule management and validation.
 *
 * This plugin enables projects to define and enforce architectural rules,
 * ensuring code consistency with the adopted architectural patterns and templates.
 *
 * Features:
 * - Define custom rulesets for architectural validation
 * - Support for dependency rules (forbidden/required dependencies)
 * - Support for naming conventions
 * - Support for structural rules
 * - Extensible with custom validators
 * - Multiple report formats (text, JSON, HTML)
 * - Configurable violation handling (warn/fail)
 *
 * Example configuration:
 * ```yaml
 * architecture:
 *   enabled: true
 *   rulesets:
 *     layered:
 *       enabled: true
 *       description: "Layered architecture rules"
 *       rules:
 *         - id: "no-direct-db-access"
 *           description: "Controllers should not directly access database"
 *           type: "dependency"
 *           pattern: ".*Controller.*"
 *           forbidden: [".*Repository.*", ".*DAO.*"]
 *         - id: "service-layer-required"
 *           description: "Controllers must use service layer"
 *           type: "dependency"
 *           pattern: ".*Controller.*"
 *           required: [".*Service.*"]
 *     naming:
 *       enabled: true
 *       description: "Naming convention rules"
 *       rules:
 *         - id: "controller-naming"
 *           description: "Controllers must end with Controller"
 *           type: "naming"
 *           pattern: ".*Controller"
 *           paths: ["src/main/.*Controller\\..*"]
 *   onViolation: "warn"  # or "fail"
 *   reportFormat: "text"  # or "json", "html"
 *   strict: false
 * ```
 *
 * Tasks:
 * - architecture-validate: Validates the project against defined architectural rules
 */
class ArchitecturePlugin : ArchitectPlugin<ArchitectureContext> {
    override val id = "architecture-plugin"
    override val contextKey: String = "architecture"
    override val ctxClass: Class<ArchitectureContext> = ArchitectureContext::class.java
    override var context: ArchitectureContext = ArchitectureContext()

    override fun register(registry: TaskRegistry) {
        registry.add(ArchitectureTask(CodeWorkflow.VERIFY, context))
    }
}
