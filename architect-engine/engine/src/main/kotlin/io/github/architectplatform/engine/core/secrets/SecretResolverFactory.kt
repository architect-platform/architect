package io.github.architectplatform.engine.core.secrets

import io.github.architectplatform.core.secrets.CompositeSecretResolver
import io.github.architectplatform.core.secrets.SecretResolver
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class SecretResolverFactory {
  @Singleton
  fun secretResolver(): SecretResolver = CompositeSecretResolver.default()
}
