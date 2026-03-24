package io.github.architectplatform.core.events

/**
 * A simple functional event bus. Implementations publish events to subscribers.
 *
 * The host runtime provides the concrete implementation.
 */
typealias EventBus<T> = (T) -> Unit
