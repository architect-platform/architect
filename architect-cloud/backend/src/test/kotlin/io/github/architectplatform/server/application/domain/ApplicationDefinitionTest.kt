package io.github.architectplatform.server.application.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ApplicationDefinition domain model.
 * Tests business logic for application definitions including environment management,
 * tagging, and multi-cloud support.
 */
class ApplicationDefinitionTest {
    
    @Test
    fun `should create application definition with default values`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0"
        )
        
        assertEquals("app-1", app.id)
        assertEquals("my-app", app.name)
        assertEquals(1, app.instances)
        assertEquals(ApplicationType.APPLICATION, app.type)
        assertEquals(DeploymentEnvironment.DEVELOPMENT, app.targetEnvironment)
        assertTrue(app.tags.isEmpty())
        assertNull(app.cloudProvider)
    }
    
    @Test
    fun `should create application definition with all fields`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            type = ApplicationType.DATABASE,
            image = "postgres:14",
            instances = 3,
            environment = mapOf("DB_NAME" to "mydb"),
            exposedPorts = listOf(ExposedPort(5432, "TCP", true)),
            targetEnvironment = DeploymentEnvironment.PRODUCTION,
            tags = mapOf("team" to "backend", "cost-center" to "engineering"),
            cloudProvider = CloudProvider.AWS
        )
        
        assertEquals(ApplicationType.DATABASE, app.type)
        assertEquals(3, app.instances)
        assertEquals(DeploymentEnvironment.PRODUCTION, app.targetEnvironment)
        assertEquals(CloudProvider.AWS, app.cloudProvider)
        assertEquals(2, app.tags.size)
        assertEquals("backend", app.tags["team"])
    }
    
    @Test
    fun `withTags should add new tags`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            tags = mapOf("env" to "dev")
        )
        
        val updated = app.withTags(mapOf("version" to "1.0.0", "team" to "backend"))
        
        assertEquals(3, updated.tags.size)
        assertEquals("dev", updated.tags["env"])
        assertEquals("1.0.0", updated.tags["version"])
        assertEquals("backend", updated.tags["team"])
        assertTrue(updated.updatedAt.isAfter(app.updatedAt))
    }
    
    @Test
    fun `withTags should override existing tags`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            tags = mapOf("env" to "dev")
        )
        
        val updated = app.withTags(mapOf("env" to "prod"))
        
        assertEquals(1, updated.tags.size)
        assertEquals("prod", updated.tags["env"])
    }
    
    @Test
    fun `withEnvironmentOverrides should add environment variables`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            environment = mapOf("PORT" to "8080")
        )
        
        val updated = app.withEnvironmentOverrides(mapOf("DB_HOST" to "localhost", "DB_PORT" to "5432"))
        
        assertEquals(3, updated.environment.size)
        assertEquals("8080", updated.environment["PORT"])
        assertEquals("localhost", updated.environment["DB_HOST"])
        assertEquals("5432", updated.environment["DB_PORT"])
    }
    
    @Test
    fun `withEnvironmentOverrides should override existing variables`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            environment = mapOf("PORT" to "8080")
        )
        
        val updated = app.withEnvironmentOverrides(mapOf("PORT" to "9090"))
        
        assertEquals(1, updated.environment.size)
        assertEquals("9090", updated.environment["PORT"])
    }
    
    @Test
    fun `getDependenciesInOrder should return required dependencies first`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            dependencies = listOf(
                Dependency("optional-service", required = false),
                Dependency("database", required = true),
                Dependency("cache", required = true)
            )
        )
        
        val ordered = app.getDependenciesInOrder()
        
        assertEquals(3, ordered.size)
        assertEquals("database", ordered[0])
        assertEquals("cache", ordered[1])
        assertEquals("optional-service", ordered[2])
    }
    
    @Test
    fun `toVariableMap should include all relevant fields`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            instances = 2,
            environment = mapOf("PORT" to "8080"),
            exposedPorts = listOf(ExposedPort(8080, "TCP", true)),
            targetEnvironment = DeploymentEnvironment.PRODUCTION,
            cloudProvider = CloudProvider.KUBERNETES,
            tags = mapOf("team" to "backend")
        )
        
        val vars = app.toVariableMap()
        
        assertEquals("my-app", vars["name"])
        assertEquals("1.0.0", vars["version"])
        assertEquals(2, vars["instances"])
        assertEquals("production", vars["targetEnvironment"])
        assertEquals("kubernetes", vars["cloudProvider"])
        assertTrue(vars.containsKey("ports"))
        assertTrue(vars.containsKey("tags"))
    }
    
    @Test
    fun `update should modify fields and update timestamp`() {
        val app = ApplicationDefinition(
            id = "app-1",
            name = "my-app",
            version = "1.0.0",
            image = "myapp:1.0.0",
            instances = 1
        )
        
        val updated = app.update(
            instances = 3,
            targetEnvironment = DeploymentEnvironment.PRODUCTION
        )
        
        assertEquals(3, updated.instances)
        assertEquals(DeploymentEnvironment.PRODUCTION, updated.targetEnvironment)
        assertTrue(updated.updatedAt.isAfter(app.updatedAt))
    }
}
