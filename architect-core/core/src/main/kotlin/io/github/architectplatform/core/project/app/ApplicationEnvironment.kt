package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.logging.ArchitectLogger
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.core.execution.TaskPermissionScope
import io.github.architectplatform.core.events.EventBus
import io.github.architectplatform.core.logging.Slf4jArchitectLogger
import io.github.architectplatform.core.secrets.CompositeSecretResolver
import io.github.architectplatform.core.secrets.SecretResolver
import jakarta.inject.Singleton

@Singleton
class ApplicationEnvironment(
    private val services: Map<Class<*>, Any> = emptyMap(),
    private val eventBus: EventBus<Any> = {},
    private val activeProfile: String = "default",
    private val secretResolver: SecretResolver = CompositeSecretResolver.default(),
) : Environment {

    private val subscribers = mutableMapOf<Class<*>, MutableList<(Any) -> Unit>>()

    override fun <T> service(type: Class<T>): T {
        val service =
            services[type]
                ?: throw IllegalArgumentException("Service of type ${type.name} not registered in environment")
        if (!type.isInstance(service)) {
            throw IllegalArgumentException("Service of type ${type.name} not registered in environment")
        }
        return type.cast(service)
    }

    override fun publish(event: Any) {
        eventBus(event)
        // Dispatch to type-matched subscribers
        subscribers.forEach { (type, handlers) ->
            if (type.isInstance(event)) {
                handlers.forEach { handler -> handler(event) }
            }
        }
    }

    override fun secret(name: String): String? = secretResolver.resolve(name, TaskPermissionScope.current()?.projectDir)

    override fun profile(): String = activeProfile

    override fun logger(tag: String): ArchitectLogger = Slf4jArchitectLogger(tag)

    @Suppress("UNCHECKED_CAST")
    override fun <E> subscribe(
        type: Class<E>,
        handler: (E) -> Unit,
    ) {
        subscribers.getOrPut(type) { mutableListOf() }.add(handler as (Any) -> Unit)
    }
}
