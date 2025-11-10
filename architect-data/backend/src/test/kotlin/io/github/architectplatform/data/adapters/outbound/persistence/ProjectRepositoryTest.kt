package io.github.architectplatform.data.adapters.outbound.persistence

import io.github.architectplatform.data.application.domain.Project
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import reactor.test.StepVerifier

/**
 * Integration tests for ProjectRepository.
 */
@MicronautTest
class ProjectRepositoryTest {
    
    @Inject
    lateinit var repository: ProjectRepository
    
    @Inject
    lateinit var adapter: ProjectPersistenceAdapter
    
    @Test
    fun `should save and retrieve project`() {
        val project = Project(
            id = "test-proj-1",
            name = "test-project",
            path = "/path/to/project",
            engineId = "engine-1",
            description = "Test project"
        )
        
        StepVerifier.create(adapter.save(project))
            .expectNextMatches { it.id == project.id }
            .verifyComplete()
        
        StepVerifier.create(adapter.findById("test-proj-1"))
            .expectNextMatches { 
                it.name == "test-project" && it.engineId == "engine-1" 
            }
            .verifyComplete()
    }
    
    @Test
    fun `should find projects by engine`() {
        val project = Project(
            id = "test-proj-2",
            name = "project-2",
            path = "/path/2",
            engineId = "engine-test"
        )
        
        adapter.save(project).block()
        
        StepVerifier.create(adapter.findByEngineId("engine-test"))
            .expectNextMatches { it.engineId == "engine-test" }
            .thenCancel()
            .verify()
    }
    
    @Test
    fun `should delete project`() {
        val project = Project(
            id = "test-proj-3",
            name = "project-3",
            path = "/path/3",
            engineId = "engine-1"
        )
        
        adapter.save(project).block()
        
        StepVerifier.create(adapter.deleteById("test-proj-3"))
            .verifyComplete()
        
        StepVerifier.create(adapter.findById("test-proj-3"))
            .verifyComplete()
    }
}
