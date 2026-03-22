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
        private val BASE_KNOWN_KEYS = setOf("project", "plugins", "tasks", "profiles", "watch")
    }

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

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }
}
