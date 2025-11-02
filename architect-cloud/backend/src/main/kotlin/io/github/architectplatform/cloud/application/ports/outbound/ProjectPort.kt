package io.github.architectplatform.cloud.application.ports.outbound

import io.github.architectplatform.cloud.application.domain.Project

/**
 * Outbound port for project persistence operations.
 * Defines the contract for project repository implementations.
 */
interface ProjectPort {
    fun save(project: Project): Project
    fun findById(id: String): Project?
    fun findAll(): List<Project>
    fun findByEngineId(engineId: String): List<Project>
    fun findByName(name: String): Project?
}
