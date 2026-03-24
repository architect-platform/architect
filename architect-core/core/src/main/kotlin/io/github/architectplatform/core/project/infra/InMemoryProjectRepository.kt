package io.github.architectplatform.core.project.infra

import io.github.architectplatform.core.project.app.repositories.ProjectRepository
import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.utils.InMemoryRepository
import jakarta.inject.Singleton

@Singleton class InMemoryProjectRepository : ProjectRepository, InMemoryRepository<Project>()
