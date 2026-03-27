package io.github.architectplatform.cli.serde

import io.github.architectplatform.core.domain.events.ExecutionCancelledEvent
import io.github.architectplatform.core.domain.events.ExecutionCompletedEvent
import io.github.architectplatform.core.domain.events.ExecutionFailedEvent
import io.github.architectplatform.core.domain.events.ExecutionStartedEvent
import io.github.architectplatform.core.domain.events.TaskCompletedEvent
import io.github.architectplatform.core.domain.events.TaskFailedEvent
import io.github.architectplatform.core.domain.events.TaskOutputEvent
import io.github.architectplatform.core.domain.events.TaskRetryingEvent
import io.github.architectplatform.core.domain.events.TaskSkippedEvent
import io.github.architectplatform.core.domain.events.TaskStartedEvent
import io.github.architectplatform.core.domain.events.TypedArchitectEvent
import io.micronaut.serde.annotation.SerdeImport

@SerdeImport(TypedArchitectEvent::class)
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
class SerdeImports
