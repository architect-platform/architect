package io.github.architectplatform.core.plugin.domain.events

import io.github.architectplatform.core.domain.events.AbstractArchitectEvent

data class ArchitectEventDTO<Model : Any>(
    override val id: String,
    override val event: Model? = null,
) : AbstractArchitectEvent<Model>(id, event)
