package io.github.architectplatform.agent.common.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for AgentConfig domain model.
 */
class AgentConfigTest {
    
    @Test
    fun `should create agent config with default values`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            serverUrl = "http://localhost:8080"
        )
        
        assertEquals("agent-1", config.id)
        assertEquals(AgentType.KUBERNETES, config.agentType)
        assertEquals("http://localhost:8080", config.serverUrl)
        assertEquals(listOf("development"), config.supportedEnvironments)
        assertNull(config.cloudProvider)
        assertNull(config.region)
        assertEquals(30, config.heartbeatIntervalSeconds)
        assertTrue(config.capabilities.isEmpty())
        assertTrue(config.metadata.isEmpty())
    }
    
    @Test
    fun `should create agent config with all fields`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.DOCKER_COMPOSE,
            serverUrl = "https://api.example.com",
            supportedEnvironments = listOf("development", "staging"),
            cloudProvider = "aws",
            region = "us-east-1",
            heartbeatIntervalSeconds = 60,
            capabilities = listOf("autoscaling", "load-balancing"),
            metadata = mapOf("version" to "1.0.0")
        )
        
        assertEquals(2, config.supportedEnvironments.size)
        assertEquals("aws", config.cloudProvider)
        assertEquals("us-east-1", config.region)
        assertEquals(60, config.heartbeatIntervalSeconds)
        assertEquals(2, config.capabilities.size)
    }
    
    @Test
    fun `validate should return success for valid config`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            serverUrl = "http://localhost:8080",
            heartbeatIntervalSeconds = 30
        )
        
        val result = config.validate()
        
        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `validate should fail for blank id`() {
        val config = AgentConfig(
            id = "",
            agentType = AgentType.KUBERNETES,
            serverUrl = "http://localhost:8080"
        )
        
        val result = config.validate()
        
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("Agent ID") })
    }
    
    @Test
    fun `validate should fail for blank server URL`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            serverUrl = ""
        )
        
        val result = config.validate()
        
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("Server URL cannot be blank") })
    }
    
    @Test
    fun `validate should fail for invalid server URL protocol`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            serverUrl = "ftp://localhost:8080"
        )
        
        val result = config.validate()
        
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("http://") })
    }
    
    @Test
    fun `validate should fail for heartbeat interval less than 10 seconds`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            serverUrl = "http://localhost:8080",
            heartbeatIntervalSeconds = 5
        )
        
        val result = config.validate()
        
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("Heartbeat interval") })
    }
    
    @Test
    fun `validate should fail for empty supported environments`() {
        val config = AgentConfig(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            serverUrl = "http://localhost:8080",
            supportedEnvironments = emptyList()
        )
        
        val result = config.validate()
        
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("supported environment") })
    }
    
    @Test
    fun `validate should return multiple errors for invalid config`() {
        val config = AgentConfig(
            id = "",
            agentType = AgentType.KUBERNETES,
            serverUrl = "",
            heartbeatIntervalSeconds = 5,
            supportedEnvironments = emptyList()
        )
        
        val result = config.validate()
        
        assertFalse(result.valid)
        assertTrue(result.errors.size >= 4)
    }
    
    @Test
    fun `fromEnvironment should create config from environment variables`() {
        // This test relies on system environment variables
        // In a real scenario, you'd mock System.getenv
        val config = AgentConfig.fromEnvironment()
        
        assertNotNull(config.id)
        assertNotNull(config.agentType)
        assertNotNull(config.serverUrl)
        assertFalse(config.supportedEnvironments.isEmpty())
    }
    
    @Test
    fun `ValidationResult success should create valid result`() {
        val result = ValidationResult.success()
        
        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `ValidationResult failure should create invalid result with errors`() {
        val errors = listOf("Error 1", "Error 2")
        val result = ValidationResult.failure(errors)
        
        assertFalse(result.valid)
        assertEquals(2, result.errors.size)
    }
}
