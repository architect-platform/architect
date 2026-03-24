package io.github.architectplatform.engine.serde

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.history.domain.ExecutionRecord
import io.github.architectplatform.engine.core.plugin.domain.events.ArchitectEventDTO
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents
import io.github.architectplatform.engine.core.tasks.domain.events.ExecutionEvents
import io.github.architectplatform.engine.core.tasks.domain.events.TaskEvents
import io.github.architectplatform.engine.core.tasks.interfaces.dto.TaskDTO
import io.github.architectplatform.engine.core.tasks.interfaces.dto.TaskPlanDTO
import io.github.architectplatform.engine.core.tasks.interfaces.dto.TaskPlanStepDTO
import io.github.architectplatform.engine.core.tasks.interfaces.dto.TaskResultDTO
import io.micronaut.serde.annotation.SerdeImport

/**
 * Centralized Micronaut Serde import declarations for core domain types.
 *
 * Core classes live in architect-core and cannot depend on Micronaut,
 * so we declare their serialization support here in the engine host.
 */
@SerdeImport(TaskResult::class)
@SerdeImport(ExecutionRecord::class)
@SerdeImport(ArchitectEventDTO::class)
@SerdeImport(PluginEvents.PluginEventDTO::class)
@SerdeImport(ExecutionEvents.ExecutionEventDTO::class)
@SerdeImport(TaskEvents.TaskEventDTO::class)
@SerdeImport(TaskDTO::class)
@SerdeImport(TaskPlanDTO::class)
@SerdeImport(TaskPlanStepDTO::class)
@SerdeImport(TaskResultDTO::class)
class SerdeImports
