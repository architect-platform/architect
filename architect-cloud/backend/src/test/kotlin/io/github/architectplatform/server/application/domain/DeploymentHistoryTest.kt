package io.github.architectplatform.server.application.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for DeploymentHistory domain model.
 */
class DeploymentHistoryTest {
    
    @Test
    fun `should create deployment history`() {
        val history = DeploymentHistory(
            id = "hist-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            version = "1.0.0",
            deploymentCommandId = "cmd-1",
            operation = DeploymentOperation.APPLY,
            environment = "production",
            agentId = "agent-1",
            status = CommandStatus.PENDING,
            success = false
        )
        
        assertEquals("hist-1", history.id)
        assertEquals("app-1", history.applicationDefinitionId)
        assertEquals(DeploymentOperation.APPLY, history.operation)
        assertEquals(CommandStatus.PENDING, history.status)
        assertFalse(history.success)
        assertNull(history.completedAt)
        assertNull(history.duration)
    }
    
    @Test
    fun `complete should update status and calculate duration`() {
        val history = DeploymentHistory(
            id = "hist-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            version = "1.0.0",
            deploymentCommandId = "cmd-1",
            operation = DeploymentOperation.APPLY,
            environment = "production",
            agentId = "agent-1",
            status = CommandStatus.SENT,
            success = false
        )
        
        Thread.sleep(100)
        val completed = history.complete(true, "Deployment successful", "http://my-app.example.com")
        
        assertEquals(CommandStatus.COMPLETED, completed.status)
        assertTrue(completed.success)
        assertEquals("Deployment successful", completed.message)
        assertEquals("http://my-app.example.com", completed.deploymentUrl)
        assertNotNull(completed.completedAt)
        assertNotNull(completed.duration)
        assertTrue(completed.duration!! >= 0)
    }
    
    @Test
    fun `complete with failure should set FAILED status`() {
        val history = DeploymentHistory(
            id = "hist-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            version = "1.0.0",
            deploymentCommandId = "cmd-1",
            operation = DeploymentOperation.APPLY,
            environment = "production",
            agentId = "agent-1",
            status = CommandStatus.SENT,
            success = false
        )
        
        val failed = history.complete(false, "Deployment failed")
        
        assertEquals(CommandStatus.FAILED, failed.status)
        assertFalse(failed.success)
        assertEquals("Deployment failed", failed.message)
    }
    
    @Test
    fun `fromDeploymentCommand should create history from command`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = mapOf("version" to "1.0.0"),
            operation = DeploymentOperation.APPLY,
            status = CommandStatus.COMPLETED,
            targetEnvironment = "production",
            deploymentVersion = 2,
            tags = mapOf("release" to "v1.0.0"),
            result = DeploymentResult(
                success = true,
                message = "Deployed successfully",
                deploymentUrl = "http://my-app.example.com"
            ),
            completedAt = java.time.Instant.now()
        )
        
        val history = DeploymentHistory.fromDeploymentCommand(command)
        
        assertEquals("cmd-1-history", history.id)
        assertEquals("app-1", history.applicationDefinitionId)
        assertEquals("my-app", history.applicationName)
        assertEquals("2", history.version)
        assertEquals("cmd-1", history.deploymentCommandId)
        assertEquals(DeploymentOperation.APPLY, history.operation)
        assertEquals("production", history.environment)
        assertEquals("agent-1", history.agentId)
        assertEquals(CommandStatus.COMPLETED, history.status)
        assertTrue(history.success)
        assertEquals("Deployed successfully", history.message)
        assertEquals("http://my-app.example.com", history.deploymentUrl)
        assertEquals(mapOf("release" to "v1.0.0"), history.tags)
    }
    
    @Test
    fun `fromDeploymentCommand should handle command without result`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            status = CommandStatus.PENDING,
            targetEnvironment = "development"
        )
        
        val history = DeploymentHistory.fromDeploymentCommand(command)
        
        assertFalse(history.success)
        assertNull(history.message)
        assertNull(history.deploymentUrl)
    }
}
