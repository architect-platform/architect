package io.github.architectplatform.server.application.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for Agent domain model.
 * Tests business logic for agent management including environment support.
 */
class AgentTest {
    
    @Test
    fun `should create agent with default values`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES
        )
        
        assertEquals("agent-1", agent.id)
        assertEquals(AgentType.KUBERNETES, agent.agentType)
        assertNull(agent.namespace)
        assertTrue(agent.capabilities.isEmpty())
        assertEquals(listOf("development"), agent.supportedEnvironments)
        assertNull(agent.cloudProvider)
        assertNull(agent.region)
        assertEquals(AgentStatus.HEALTHY, agent.status)
    }
    
    @Test
    fun `should create agent with all fields`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            namespace = "production",
            capabilities = listOf("autoscaling", "load-balancing"),
            supportedEnvironments = listOf("staging", "production"),
            cloudProvider = "aws",
            region = "us-east-1",
            metadata = mapOf("cluster" to "prod-cluster-1"),
            status = AgentStatus.HEALTHY
        )
        
        assertEquals("production", agent.namespace)
        assertEquals(2, agent.capabilities.size)
        assertEquals(2, agent.supportedEnvironments.size)
        assertEquals("aws", agent.cloudProvider)
        assertEquals("us-east-1", agent.region)
    }
    
    @Test
    fun `updateHeartbeat should update timestamp and status`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.DOCKER_COMPOSE,
            status = AgentStatus.DEGRADED
        )
        
        Thread.sleep(10)
        val updated = agent.updateHeartbeat(AgentStatus.HEALTHY)
        
        assertEquals(AgentStatus.HEALTHY, updated.status)
        assertTrue(updated.lastHeartbeat.isAfter(agent.lastHeartbeat))
    }
    
    @Test
    fun `markOffline should set status to OFFLINE`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            status = AgentStatus.HEALTHY
        )
        
        val offline = agent.markOffline()
        
        assertEquals(AgentStatus.OFFLINE, offline.status)
    }
    
    @Test
    fun `isHealthy should return true for recent heartbeat`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            status = AgentStatus.HEALTHY
        )
        
        assertTrue(agent.isHealthy())
    }
    
    @Test
    fun `isHealthy should return false for old heartbeat`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            status = AgentStatus.HEALTHY,
            lastHeartbeat = java.time.Instant.now().minusSeconds(400)
        )
        
        assertFalse(agent.isHealthy())
    }
    
    @Test
    fun `isHealthy should return false for non-healthy status`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            status = AgentStatus.OFFLINE
        )
        
        assertFalse(agent.isHealthy())
    }
    
    @Test
    fun `supportsEnvironment should return true for supported environment`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            supportedEnvironments = listOf("development", "staging", "production")
        )
        
        assertTrue(agent.supportsEnvironment("production"))
        assertTrue(agent.supportsEnvironment("staging"))
    }
    
    @Test
    fun `supportsEnvironment should return false for unsupported environment`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            supportedEnvironments = listOf("development")
        )
        
        assertFalse(agent.supportsEnvironment("production"))
    }
    
    @Test
    fun `supportsCloudProvider should return true when no provider specified`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            cloudProvider = null
        )
        
        assertTrue(agent.supportsCloudProvider("aws"))
        assertTrue(agent.supportsCloudProvider("gcp"))
    }
    
    @Test
    fun `supportsCloudProvider should return true for matching provider`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            cloudProvider = "aws"
        )
        
        assertTrue(agent.supportsCloudProvider("aws"))
    }
    
    @Test
    fun `supportsCloudProvider should return false for non-matching provider`() {
        val agent = Agent(
            id = "agent-1",
            agentType = AgentType.KUBERNETES,
            cloudProvider = "aws"
        )
        
        assertFalse(agent.supportsCloudProvider("gcp"))
    }
}
