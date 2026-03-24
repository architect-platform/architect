package io.github.architectplatform.engine.core.secrets

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class SecretResolverFactory {
  @Singleton
  fun secretResolver(): SecretResolver = CompositeSecretResolver.default()
}