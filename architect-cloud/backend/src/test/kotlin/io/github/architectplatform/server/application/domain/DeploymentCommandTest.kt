package io.github.architectplatform.server.application.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for DeploymentCommand domain model.
 * Tests business logic for deployment commands including rollback support.
 */
class DeploymentCommandTest {
    
    @Test
    fun `should create deployment command with default values`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = mapOf("name" to "my-app")
        )
        
        assertEquals("cmd-1", command.id)
        assertEquals(DeploymentOperation.APPLY, command.operation)
        assertEquals(CommandStatus.PENDING, command.status)
        assertEquals("development", command.targetEnvironment)
        assertEquals(1, command.deploymentVersion)
        assertNull(command.previousCommandId)
        assertNull(command.sentAt)
        assertNull(command.completedAt)
        assertFalse(command.isRollback())
    }
    
    @Test
    fun `markSent should update status and timestamp`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap()
        )
        
        val sent = command.markSent()
        
        assertEquals(CommandStatus.SENT, sent.status)
        assertNotNull(sent.sentAt)
        assertTrue(sent.sentAt!!.isAfter(command.createdAt))
    }
    
    @Test
    fun `markCompleted should update status with result`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            status = CommandStatus.SENT
        )
        
        val result = DeploymentResult(
            success = true,
            message = "Deployment successful",
            deploymentUrl = "http://my-app.example.com"
        )
        
        val completed = command.markCompleted(result)
        
        assertEquals(CommandStatus.COMPLETED, completed.status)
        assertEquals(result, completed.result)
        assertNotNull(completed.completedAt)
        assertTrue(completed.isFinal())
    }
    
    @Test
    fun `markFailed should create failure result`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            status = CommandStatus.SENT
        )
        
        val failed = command.markFailed("Deployment failed: resource not found")
        
        assertEquals(CommandStatus.FAILED, failed.status)
        assertFalse(failed.result?.success ?: true)
        assertEquals("Deployment failed: resource not found", failed.result?.message)
        assertNotNull(failed.completedAt)
        assertTrue(failed.isFinal())
    }
    
    @Test
    fun `markCancelled should update status`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap()
        )
        
        val cancelled = command.markCancelled()
        
        assertEquals(CommandStatus.CANCELLED, cancelled.status)
        assertNotNull(cancelled.completedAt)
        assertTrue(cancelled.isFinal())
    }
    
    @Test
    fun `createRollbackCommand should create new rollback command`() {
        val original = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = mapOf("version" to "1.0.0"),
            targetEnvironment = "production",
            deploymentVersion = 5,
            tags = mapOf("type" to "release"),
            status = CommandStatus.COMPLETED
        )
        
        val rollback = original.createRollbackCommand("cmd-rollback-1")
        
        assertEquals("cmd-rollback-1", rollback.id)
        assertEquals(DeploymentOperation.ROLLBACK, rollback.operation)
        assertEquals("cmd-1", rollback.previousCommandId)
        assertEquals(CommandStatus.PENDING, rollback.status)
        assertEquals(original.agentId, rollback.agentId)
        assertEquals(original.applicationDefinitionId, rollback.applicationDefinitionId)
        assertEquals(original.templates, rollback.templates)
        assertEquals(original.variables, rollback.variables)
        assertNull(rollback.sentAt)
        assertNull(rollback.completedAt)
        assertNull(rollback.result)
        assertTrue(rollback.isRollback())
    }
    
    @Test
    fun `isRollback should return true for rollback operations`() {
        val rollback = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            operation = DeploymentOperation.ROLLBACK
        )
        
        assertTrue(rollback.isRollback())
    }
    
    @Test
    fun `isRollback should return false for non-rollback operations`() {
        val command = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            operation = DeploymentOperation.APPLY
        )
        
        assertFalse(command.isRollback())
    }
    
    @Test
    fun `isFinal should return true for terminal states`() {
        val completed = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            status = CommandStatus.COMPLETED
        )
        
        val failed = completed.copy(status = CommandStatus.FAILED)
        val cancelled = completed.copy(status = CommandStatus.CANCELLED)
        
        assertTrue(completed.isFinal())
        assertTrue(failed.isFinal())
        assertTrue(cancelled.isFinal())
    }
    
    @Test
    fun `isFinal should return false for non-terminal states`() {
        val pending = DeploymentCommand(
            id = "cmd-1",
            agentId = "agent-1",
            applicationDefinitionId = "app-1",
            applicationName = "my-app",
            templates = listOf("deployment.yaml"),
            variables = emptyMap(),
            status = CommandStatus.PENDING
        )
        
        val sent = pending.copy(status = CommandStatus.SENT)
        
        assertFalse(pending.isFinal())
        assertFalse(sent.isFinal())
    }
}
