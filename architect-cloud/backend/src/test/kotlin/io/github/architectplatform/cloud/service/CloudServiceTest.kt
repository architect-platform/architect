package io.github.architectplatform.cloud.service

import io.github.architectplatform.cloud.domain.EngineStatus
import io.github.architectplatform.cloud.dto.RegisterEngineRequest
import io.github.architectplatform.cloud.dto.RegisterProjectRequest
import io.github.architectplatform.cloud.dto.ReportExecutionRequest
import io.github.architectplatform.cloud.dto.ReportEventRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@MicronautTest
class CloudServiceTest {
    
    @Inject
    lateinit var cloudService: CloudService
    
    @Test
    fun `should register engine`() {
        val request = RegisterEngineRequest(
            id = "test-engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        val engine = cloudService.registerEngine(request)
        
        assertNotNull(engine)
        assertEquals("test-engine-1", engine.id)
        assertEquals("localhost", engine.hostname)
        assertEquals(9292, engine.port)
        assertEquals("1.0.0", engine.version)
        assertEquals(EngineStatus.ACTIVE, engine.status)
    }
    
    @Test
    fun `should register project`() {
        // First register engine
        val engineRequest = RegisterEngineRequest(
            id = "test-engine-2",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        cloudService.registerEngine(engineRequest)
        
        // Then register project
        val projectRequest = RegisterProjectRequest(
            id = "test-project-1",
            name = "Test Project",
            path = "/path/to/project",
            engineId = "test-engine-2",
            description = "A test project"
        )
        
        val project = cloudService.registerProject(projectRequest)
        
        assertNotNull(project)
        assertEquals("test-project-1", project.id)
        assertEquals("Test Project", project.name)
        assertEquals("/path/to/project", project.path)
        assertEquals("test-engine-2", project.engineId)
        assertEquals("A test project", project.description)
    }
    
    @Test
    fun `should report execution`() {
        // Setup
        val engineRequest = RegisterEngineRequest(
            id = "test-engine-3",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        cloudService.registerEngine(engineRequest)
        
        val projectRequest = RegisterProjectRequest(
            id = "test-project-2",
            name = "Test Project 2",
            path = "/path/to/project2",
            engineId = "test-engine-3"
        )
        cloudService.registerProject(projectRequest)
        
        // Report execution
        val executionRequest = ReportExecutionRequest(
            id = "exec-1",
            projectId = "test-project-2",
            engineId = "test-engine-3",
            taskId = "test-task",
            status = "STARTED",
            message = "Task started"
        )
        
        val execution = cloudService.reportExecution(executionRequest)
        
        assertNotNull(execution)
        assertEquals("exec-1", execution.id)
        assertEquals("test-project-2", execution.projectId)
        assertEquals("test-engine-3", execution.engineId)
        assertEquals("test-task", execution.taskId)
    }
    
    @Test
    fun `should report event`() {
        // Setup
        val engineRequest = RegisterEngineRequest(
            id = "test-engine-4",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        cloudService.registerEngine(engineRequest)
        
        val projectRequest = RegisterProjectRequest(
            id = "test-project-3",
            name = "Test Project 3",
            path = "/path/to/project3",
            engineId = "test-engine-4"
        )
        cloudService.registerProject(projectRequest)
        
        val executionRequest = ReportExecutionRequest(
            id = "exec-2",
            projectId = "test-project-3",
            engineId = "test-engine-4",
            taskId = "test-task-2",
            status = "STARTED"
        )
        cloudService.reportExecution(executionRequest)
        
        // Report event
        val eventRequest = ReportEventRequest(
            id = "event-1",
            executionId = "exec-2",
            eventType = "task.started",
            taskId = "test-task-2",
            message = "Task started",
            success = true
        )
        
        val event = cloudService.reportEvent(eventRequest)
        
        assertNotNull(event)
        assertEquals("event-1", event.id)
        assertEquals("exec-2", event.executionId)
        assertEquals("task.started", event.eventType)
        assertEquals("test-task-2", event.taskId)
        assertTrue(event.success)
    }
    
    @Test
    fun `should get projects by engine`() {
        // Setup
        val engineRequest = RegisterEngineRequest(
            id = "test-engine-5",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        cloudService.registerEngine(engineRequest)
        
        val project1Request = RegisterProjectRequest(
            id = "test-project-4",
            name = "Test Project 4",
            path = "/path/to/project4",
            engineId = "test-engine-5"
        )
        cloudService.registerProject(project1Request)
        
        val project2Request = RegisterProjectRequest(
            id = "test-project-5",
            name = "Test Project 5",
            path = "/path/to/project5",
            engineId = "test-engine-5"
        )
        cloudService.registerProject(project2Request)
        
        // Get projects
        val projects = cloudService.getProjectsByEngine("test-engine-5")
        
        assertEquals(2, projects.size)
        assertTrue(projects.any { it.name == "Test Project 4" })
        assertTrue(projects.any { it.name == "Test Project 5" })
    }
}
