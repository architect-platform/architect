package io.github.architectplatform.data.application.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for Project domain model.
 */
class ProjectTest {
    
    @Test
    fun `should create project with all fields`() {
        val project = Project(
            id = "proj-1",
            name = "my-project",
            path = "/path/to/project",
            engineId = "engine-1",
            description = "Test project"
        )
        
        assertEquals("proj-1", project.id)
        assertEquals("my-project", project.name)
        assertEquals("/path/to/project", project.path)
        assertEquals("engine-1", project.engineId)
        assertEquals("Test project", project.description)
        assertNotNull(project.createdAt)
    }
    
    @Test
    fun `should create project without optional description`() {
        val project = Project(
            id = "proj-1",
            name = "my-project",
            path = "/path/to/project",
            engineId = "engine-1"
        )
        
        assertEquals("proj-1", project.id)
        assertNull(project.description)
    }
    
    @Test
    fun `project should be immutable`() {
        val project = Project(
            id = "proj-1",
            name = "my-project",
            path = "/path/to/project",
            engineId = "engine-1"
        )
        
        val updated = project.copy(description = "Updated description")
        
        assertNull(project.description)
        assertEquals("Updated description", updated.description)
        assertEquals(project.id, updated.id)
    }
}
