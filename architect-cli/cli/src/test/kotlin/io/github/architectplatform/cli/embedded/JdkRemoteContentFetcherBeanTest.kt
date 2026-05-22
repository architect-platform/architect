package io.github.architectplatform.cli.embedded

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class JdkRemoteContentFetcherBeanTest {

  @Test
  fun `application context resolves embedded executor beans`() {
    ApplicationContext.run().use { context ->
      assertDoesNotThrow {
        context.getBean(JdkRemoteContentFetcher::class.java)
      }
      assertDoesNotThrow {
        context.getBean(EmbeddedTaskExecutor::class.java)
      }
    }
  }
}
