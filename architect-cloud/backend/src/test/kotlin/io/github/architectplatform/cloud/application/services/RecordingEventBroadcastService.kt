package io.github.architectplatform.cloud.application.services

import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock

/**
 * Test helper for creating a mock EventBroadcastService and capturing broadcast calls.
 */
object EventBroadcastTestHelper {
  fun createMock(): EventBroadcastService = mock()

  fun captureCloudEvents() = argumentCaptor<CloudEvent>()
}
