package io.github.architectplatform.agent.common.domain

/**
 * Common agent configuration domain model.
 * Shared across all agent types.
 */
data class AgentConfig(
    val id: String,
    val agentType: AgentType,
    val serverUrl: String,
    val supportedEnvironments: List<String> = listOf("development"),
    val cloudProvider: String? = null,
    val region: String? = null,
    val heartbeatIntervalSeconds: Int = 30,
    val capabilities: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (id.isBlank()) {
            errors.add("Agent ID cannot be blank")
        }
        
        if (serverUrl.isBlank()) {
            errors.add("Server URL cannot be blank")
        }
        
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            errors.add("Server URL must start with http:// or https://")
        }
        
        if (heartbeatIntervalSeconds < 10) {
            errors.add("Heartbeat interval must be at least 10 seconds")
        }
        
        if (supportedEnvironments.isEmpty()) {
            errors.add("At least one supported environment must be specified")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }
    
    companion object {
        fun fromEnvironment(): AgentConfig {
            return AgentConfig(
                id = System.getenv("AGENT_ID") ?: "agent-${System.currentTimeMillis()}",
                agentType = AgentType.valueOf(System.getenv("AGENT_TYPE") ?: "GENERIC"),
                serverUrl = System.getenv("SERVER_URL") ?: "http://localhost:8080",
                supportedEnvironments = System.getenv("SUPPORTED_ENVIRONMENTS")?.split(",") ?: listOf("development"),
                cloudProvider = System.getenv("CLOUD_PROVIDER"),
                region = System.getenv("REGION"),
                heartbeatIntervalSeconds = System.getenv("HEARTBEAT_INTERVAL")?.toIntOrNull() ?: 30
            )
        }
    }
}

/**
 * Agent type enumeration
 */
enum class AgentType {
    KUBERNETES,
    DOCKER_COMPOSE,
    DOCKER_SWARM,
    AWS_ECS,
    AWS_FARGATE,
    GOOGLE_CLOUD_RUN,
    AZURE_CONTAINER_INSTANCES,
    NOMAD,
    GENERIC
}

/**
 * Validation result
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(valid = true)
        fun failure(errors: List<String>) = ValidationResult(valid = false, errors = errors)
    }
}
