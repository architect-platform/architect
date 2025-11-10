package io.github.architectplatform.agent.dockercompose.domain

/**
 * Domain model for agent configuration.
 * Encapsulates validation logic - Single Responsibility Principle.
 */
data class AgentConfig(
    val agentId: String,
    val serverUrl: String,
    val serverToken: String? = null,
    val workingDirectory: String = "/tmp/docker-compose",
    val heartbeatIntervalSeconds: Int = 30,
    val dockerComposeCommand: String = "docker-compose"
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (agentId.isBlank()) errors.add("Agent ID cannot be blank")
        if (serverUrl.isBlank()) errors.add("Server URL cannot be blank")
        if (heartbeatIntervalSeconds < 1) errors.add("Heartbeat interval must be at least 1 second")

        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(errors)
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun valid() = ValidationResult(isValid = true)
        fun invalid(errors: List<String>) = ValidationResult(isValid = false, errors = errors)
    }
}
