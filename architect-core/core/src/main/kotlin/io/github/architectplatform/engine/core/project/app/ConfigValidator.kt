package io.github.architectplatform.engine.core.project.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import io.github.architectplatform.api.core.project.Config
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.engine.core.schema.ArchitectSchemaGenerator
import jakarta.inject.Singleton

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
)

class ConfigValidationException(message: String) : RuntimeException(message)

@Singleton
class ConfigValidator {

    companion object {
        private val BASE_KNOWN_KEYS = setOf("project", "plugins", "tasks", "\$schema")
    }

    private val objectMapper = ObjectMapper().registerKotlinModule()

    /**
     * Validates the project config.
     *
     * @param config The raw project config map.
     * @param pluginContextKeys Context keys declared by loaded plugins (e.g. "gradle", "git", "docs").
     *   These are added to the known-key set so they do not produce false-positive warnings.
     */
    fun validate(config: Config, pluginContextKeys: Set<String> = emptySet()): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val projectName = config.getKey<String>("project.name")
        if (projectName.isNullOrBlank()) {
            errors.add("'project.name' is required but missing or blank")
        }

        val knownKeys = BASE_KNOWN_KEYS + pluginContextKeys
        val unknownKeys = config.keys - knownKeys
        unknownKeys.forEach { key ->
            warnings.add("Unknown top-level key '$key' in architect.yml — it will be ignored")
        }

        // If $schema is declared, validate the config against the JSON Schema
        val schemaUrl = config.getKey<String>("\$schema")
        if (schemaUrl != null) {
            errors.addAll(validateAgainstSchema(config))
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }

    private fun validateAgainstSchema(config: Config): List<String> {
        val schemaNode = ArchitectSchemaGenerator.generate()
        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        val schema = factory.getSchema(schemaNode)
        val configNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(config)
        val validationMessages = schema.validate(configNode)
        return validationMessages.map { it.message }
    }
}
