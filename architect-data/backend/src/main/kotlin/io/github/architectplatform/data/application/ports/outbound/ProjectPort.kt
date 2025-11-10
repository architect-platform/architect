package io.github.architectplatform.data.application.ports.outbound

import io.github.architectplatform.data.application.domain.Project

/**
 * Outbound port for project persistence.
 * Repository interface for project data access.
 */
interface ProjectPort {
    fun save(project: Project): Project
    fun findById(id: String): Project?
    fun findAll(): List<Project>
    fun findByEngineId(engineId: String): List<Project>
}
