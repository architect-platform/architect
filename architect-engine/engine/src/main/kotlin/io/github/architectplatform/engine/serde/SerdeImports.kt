package io.github.architectplatform.engine.serde

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.history.domain.ExecutionRecord
import io.github.architectplatform.core.plugin.domain.events.ArchitectEventDTO
import io.github.architectplatform.core.plugin.domain.events.PluginEvents
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents
import io.github.architectplatform.core.tasks.domain.events.TaskEvents
import io.github.architectplatform.core.domain.events.TypedArchitectEvent
import io.github.architectplatform.core.domain.events.TaskStartedEvent
import io.github.architectplatform.core.domain.events.TaskCompletedEvent
import io.github.architectplatform.core.domain.events.TaskFailedEvent
import io.github.architectplatform.core.domain.events.TaskOutputEvent
import io.github.architectplatform.core.domain.events.TaskRetryingEvent
import io.github.architectplatform.core.domain.events.TaskSkippedEvent
import io.github.architectplatform.core.domain.events.ExecutionStartedEvent
import io.github.architectplatform.core.domain.events.ExecutionCompletedEvent
import io.github.architectplatform.core.domain.events.ExecutionFailedEvent
import io.github.architectplatform.core.domain.events.ExecutionCancelledEvent
import io.github.architectplatform.core.domain.events.PluginLoadedEvent
import io.github.architectplatform.core.domain.events.ProjectRegisteredEvent
import io.github.architectplatform.core.tasks.dto.TaskDTO
import io.github.architectplatform.core.tasks.dto.TaskPlanDTO
import io.github.architectplatform.core.tasks.dto.TaskPlanStepDTO
import io.github.architectplatform.core.tasks.dto.TaskResultDTO
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
@SerdeImport(TypedArchitectEvent::class)
@SerdeImport(PluginEvents.PluginEventDTO::class)
@SerdeImport(ExecutionEvents.ExecutionEventDTO::class)
@SerdeImport(TaskEvents.TaskEventDTO::class)
@SerdeImport(TaskStartedEvent::class)
@SerdeImport(TaskCompletedEvent::class)
@SerdeImport(TaskFailedEvent::class)
@SerdeImport(TaskOutputEvent::class)
@SerdeImport(TaskRetryingEvent::class)
@SerdeImport(TaskSkippedEvent::class)
@SerdeImport(ExecutionStartedEvent::class)
@SerdeImport(ExecutionCompletedEvent::class)
@SerdeImport(ExecutionFailedEvent::class)
@SerdeImport(ExecutionCancelledEvent::class)
@SerdeImport(PluginLoadedEvent::class)
@SerdeImport(ProjectRegisteredEvent::class)
@SerdeImport(TaskDTO::class)
@SerdeImport(TaskPlanDTO::class)
@SerdeImport(TaskPlanStepDTO::class)
@SerdeImport(TaskResultDTO::class)
class SerdeImports
