package io.github.architectplatform.cloud.adapters.outbound.persistence

import io.github.architectplatform.cloud.adapters.outbound.persistence.entities.ProjectEntity
import io.github.architectplatform.cloud.adapters.outbound.persistence.repositories.JdbcProjectRepository
import io.github.architectplatform.cloud.application.domain.Project
import io.github.architectplatform.cloud.application.ports.outbound.ProjectPort
import jakarta.inject.Singleton

/**
 * Persistence adapter for projects.
 * Implements the outbound port using JDBC repository.
 */
@Singleton
class ProjectPersistenceAdapter(
    private val repository: JdbcProjectRepository
) : ProjectPort {
    
    override fun save(project: Project): Project {
        val entity = ProjectEntity.fromDomain(project)
        val saved = repository.save(entity)
        return saved.toDomain()
    }
    
    override fun findById(id: String): Project? {
        return repository.findById(id).map { it.toDomain() }.orElse(null)
    }
    
    override fun findAll(): List<Project> {
        return repository.findAll().map { it.toDomain() }
    }
    
    override fun findByEngineId(engineId: String): List<Project> {
        return repository.findByEngineId(engineId).map { it.toDomain() }
    }
    
    override fun findByName(name: String): Project? {
        return repository.findByName(name)?.toDomain()
    }
}
