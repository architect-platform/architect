package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.engine.core.execution.TaskPermissionScope
import io.github.architectplatform.engine.core.secrets.SecretResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.file.Paths

class ApplicationEnvironmentTest {
  @Test
  fun `secret delegates to resolver with scoped project dir`() {
    val beanContext = mock<io.micronaut.context.BeanContext>()
    val publisher = mock<io.micronaut.context.event.ApplicationEventPublisher<Any>>()
    val resolver = mock<SecretResolver>()
    whenever(beanContext.findBean(SecretResolver::class.java)).thenReturn(java.util.Optional.of(resolver))
    whenever(resolver.resolve("API_TOKEN", Paths.get("/tmp/project"))).thenReturn("resolved-secret")

    val environment = ApplicationEnvironment(beanContext, publisher)

    val secret = TaskPermissionScope.withPermissions("task", emptySet(), Paths.get("/tmp/project")) {
      environment.secret("API_TOKEN")
    }

    assertEquals("resolved-secret", secret)
  }
}