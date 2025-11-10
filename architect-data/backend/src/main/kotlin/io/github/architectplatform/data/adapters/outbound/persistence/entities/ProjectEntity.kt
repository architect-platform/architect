package io.github.architectplatform.data.adapters.outbound.persistence.entities

import io.github.architectplatform.data.application.domain.Project
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for project persistence.
 * Mapped to database table.
 */
@MappedEntity("projects")
data class ProjectEntity(
    @field:Id
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String?,
    val createdAt: Instant
) {
    companion object {
        fun fromDomain(project: Project): ProjectEntity {
            return ProjectEntity(
                id = project.id,
                name = project.name,
                path = project.path,
                engineId = project.engineId,
                description = project.description,
                createdAt = project.createdAt
            )
        }
    }
    
    fun toDomain(): Project {
        return Project(
            id = id,
            name = name,
            path = path,
            engineId = engineId,
            description = description,
            createdAt = createdAt
        )
    }
}
