package io.github.architectplatform.agent.domain

/**
 * Domain model representing agent configuration.
 * Contains settings for connecting to Architect Server and Kubernetes.
 */
data class AgentConfig(
    val agentId: String,
    val serverUrl: String,
    val serverToken: String? = null,
    val kubernetesNamespace: String = "default",
    val heartbeatIntervalSeconds: Int = 30,
    val isolateApplications: Boolean = true
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (agentId.isBlank()) {
            errors.add("Agent ID cannot be blank")
        }

        if (serverUrl.isBlank()) {
            errors.add("Server URL cannot be blank")
        }

        if (heartbeatIntervalSeconds < 1) {
            errors.add("Heartbeat interval must be at least 1 second")
        }

        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(errors)
        }
    }
}

/**
 * Result of validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun valid() = ValidationResult(isValid = true)
        fun invalid(errors: List<String>) = ValidationResult(isValid = false, errors = errors)
    }
}
