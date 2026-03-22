package io.github.architectplatform.engine.core.events

import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process event bus implementation for embedded mode.
 *
 * Subscribers are invoked synchronously in registration order.
 */
class EmbeddedEventBus<T> : (T) -> Unit {
  private val subscribers = CopyOnWriteArrayList<(T) -> Unit>()

  override fun invoke(event: T) {
    subscribers.forEach { subscriber -> subscriber(event) }
  }

  fun subscribe(handler: (T) -> Unit): () -> Unit {
    subscribers += handler
    return { subscribers -= handler }
  }

  fun clear() {
    subscribers.clear()
  }
}
