package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.Project
import io.github.architectplatform.data.application.ports.outbound.ProjectPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Unit tests for ProjectService.
 */
class ProjectServiceTest {
    
    private lateinit var projectPort: ProjectPort
    private lateinit var projectService: ProjectService
    
    @BeforeEach
    fun setup() {
        projectPort = mockk()
        projectService = ProjectService(projectPort)
    }
    
    @Test
    fun `createProject should save new project`() {
        val project = Project(
            id = "proj-1",
            name = "my-project",
            path = "/path/to/project",
            engineId = "engine-1",
            description = "Test project"
        )
        
        every { projectPort.save(any()) } returns Mono.just(project)
        
        StepVerifier.create(projectService.createProject(project))
            .expectNext(project)
            .verifyComplete()
        
        verify { projectPort.save(project) }
    }
    
    @Test
    fun `getProject should return project by id`() {
        val project = Project(
            id = "proj-1",
            name = "my-project",
            path = "/path/to/project",
            engineId = "engine-1"
        )
        
        every { projectPort.findById("proj-1") } returns Mono.just(project)
        
        StepVerifier.create(projectService.getProject("proj-1"))
            .expectNext(project)
            .verifyComplete()
    }
    
    @Test
    fun `getAllProjects should return all projects`() {
        val projects = listOf(
            Project("proj-1", "project-1", "/path/1", "engine-1"),
            Project("proj-2", "project-2", "/path/2", "engine-2")
        )
        
        every { projectPort.findAll() } returns Flux.fromIterable(projects)
        
        StepVerifier.create(projectService.getAllProjects())
            .expectNext(projects[0])
            .expectNext(projects[1])
            .verifyComplete()
    }
    
    @Test
    fun `getProjectsByEngine should return projects for specific engine`() {
        val projects = listOf(
            Project("proj-1", "project-1", "/path/1", "engine-1"),
            Project("proj-2", "project-2", "/path/2", "engine-1")
        )
        
        every { projectPort.findByEngineId("engine-1") } returns Flux.fromIterable(projects)
        
        StepVerifier.create(projectService.getProjectsByEngine("engine-1"))
            .expectNext(projects[0])
            .expectNext(projects[1])
            .verifyComplete()
        
        verify { projectPort.findByEngineId("engine-1") }
    }
    
    @Test
    fun `updateProject should save updated project`() {
        val original = Project(
            id = "proj-1",
            name = "my-project",
            path = "/path/to/project",
            engineId = "engine-1"
        )
        
        val updated = original.copy(description = "Updated description")
        
        every { projectPort.findById("proj-1") } returns Mono.just(original)
        every { projectPort.save(any()) } returns Mono.just(updated)
        
        StepVerifier.create(projectService.updateProject(updated))
            .expectNext(updated)
            .verifyComplete()
        
        verify { projectPort.save(updated) }
    }
    
    @Test
    fun `deleteProject should call port delete`() {
        every { projectPort.deleteById("proj-1") } returns Mono.empty()
        
        StepVerifier.create(projectService.deleteProject("proj-1"))
            .verifyComplete()
        
        verify(exactly = 1) { projectPort.deleteById("proj-1") }
    }
}
