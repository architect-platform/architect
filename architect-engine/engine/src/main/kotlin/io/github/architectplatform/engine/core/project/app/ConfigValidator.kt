package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.project.Config
import io.github.architectplatform.api.core.project.getKey
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
        private val KNOWN_TOP_LEVEL_KEYS = setOf("project", "plugins", "tasks")
    }

    fun validate(config: Config): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val projectName = config.getKey<String>("project.name")
        if (projectName.isNullOrBlank()) {
            errors.add("'project.name' is required but missing or blank")
        }

        val unknownKeys = config.keys - KNOWN_TOP_LEVEL_KEYS
        unknownKeys.forEach { key ->
            warnings.add("Unknown top-level key '$key' in architect.yml — it will be ignored")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }
}
