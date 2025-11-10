package io.github.architectplatform.agent.config

import io.github.architectplatform.agent.domain.AgentConfig
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.util.UUID

/**
 * Configuration properties for the Kubernetes Agent
 */
@ConfigurationProperties("agent")
class AgentConfigProperties {
    var id: String = UUID.randomUUID().toString()
    var server = ServerConfig()
    var kubernetes = KubernetesConfig()
    var heartbeat = HeartbeatConfig()

    class ServerConfig {
        var url: String = "http://localhost:8080"
        var token: String? = null
    }

    class KubernetesConfig {
        var namespace: String = "default"
        var isolateApplications: Boolean = true
    }

    class HeartbeatConfig {
        var intervalSeconds: Int = 30
    }
}

/**
 * Factory for creating AgentConfig domain object from properties
 */
@Factory
class AgentConfigFactory {

    @Singleton
    fun agentConfig(properties: AgentConfigProperties): AgentConfig {
        return AgentConfig(
            agentId = properties.id,
            serverUrl = properties.server.url,
            serverToken = properties.server.token,
            kubernetesNamespace = properties.kubernetes.namespace,
            heartbeatIntervalSeconds = properties.heartbeat.intervalSeconds,
            isolateApplications = properties.kubernetes.isolateApplications
        )
    }
}
