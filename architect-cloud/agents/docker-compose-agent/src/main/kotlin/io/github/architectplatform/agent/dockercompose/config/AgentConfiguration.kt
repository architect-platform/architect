package io.github.architectplatform.agent.dockercompose.config

import io.github.architectplatform.agent.dockercompose.domain.AgentConfig
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.util.UUID

/**
 * Configuration for Docker Compose Agent.
 * Follows Dependency Injection principle.
 */
@ConfigurationProperties("agent")
class AgentConfigProperties {
    var id: String = UUID.randomUUID().toString()
    var server = ServerConfig()
    var dockerCompose = DockerComposeConfig()
    var heartbeat = HeartbeatConfig()

    class ServerConfig {
        var url: String = "http://localhost:8080"
        var token: String? = null
    }

    class DockerComposeConfig {
        var workingDirectory: String = "/tmp/docker-compose"
        var command: String = "docker-compose"
    }

    class HeartbeatConfig {
        var intervalSeconds: Int = 30
    }
}

@Factory
class AgentConfigFactory {

    @Singleton
    fun agentConfig(properties: AgentConfigProperties): AgentConfig {
        return AgentConfig(
            agentId = properties.id,
            serverUrl = properties.server.url,
            serverToken = properties.server.token,
            workingDirectory = properties.dockerCompose.workingDirectory,
            heartbeatIntervalSeconds = properties.heartbeat.intervalSeconds,
            dockerComposeCommand = properties.dockerCompose.command
        )
    }
}
