package io.github.architectplatform.cloud.adapters.outbound.persistence.entities

import io.github.architectplatform.cloud.application.domain.Project
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

/**
 * JPA entity for project persistence.
 * Maps domain model to database table.
 */
@MappedEntity("projects")
data class ProjectEntity(
    @field:Id
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String?,
    @DateCreated
    val createdAt: Instant? = null
) {
    fun toDomain(): Project {
        return Project(
            id = id,
            name = name,
            path = path,
            engineId = engineId,
            description = description,
            createdAt = createdAt ?: Instant.now()
        )
    }
    
    companion object {
        fun fromDomain(domain: Project): ProjectEntity {
            return ProjectEntity(
                id = domain.id,
                name = domain.name,
                path = domain.path,
                engineId = domain.engineId,
                description = domain.description,
                createdAt = domain.createdAt
            )
        }
    }
}
