package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.engine.core.execution.TaskPermissionScope
import io.github.architectplatform.engine.core.events.EventBus
import io.github.architectplatform.engine.core.secrets.CompositeSecretResolver
import io.github.architectplatform.engine.core.secrets.SecretResolver
import jakarta.inject.Singleton

@Singleton
class ApplicationEnvironment(
    private val services: Map<Class<*>, Any> = emptyMap(),
    private val eventBus: EventBus<Any> = {},
    private val activeProfile: String = "default",
    private val secretResolver: SecretResolver = CompositeSecretResolver.default(),
) : Environment {

    override fun <T> service(type: Class<T>): T =
        (services[type] as? T)
            ?: throw IllegalArgumentException("Service of type ${type.name} not registered in environment")

    override fun publish(event: Any) = eventBus(event)

    override fun secret(name: String): String? = secretResolver.resolve(name, TaskPermissionScope.current()?.projectDir)

    override fun profile(): String = activeProfile
}
