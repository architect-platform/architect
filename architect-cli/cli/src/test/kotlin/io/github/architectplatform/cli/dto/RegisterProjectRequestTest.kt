package io.github.architectplatform.cli.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for RegisterProjectRequest data transfer object.
 */
class RegisterProjectRequestTest {
  @Test
  fun `RegisterProjectRequest should be created with correct values`() {
    val request = RegisterProjectRequest(name = "my-project", path = "/home/user/my-project")

    assertEquals("my-project", request.name)
    assertEquals("/home/user/my-project", request.path)
  }

  @Test
  fun `RegisterProjectRequest equality should work correctly`() {
    val request1 = RegisterProjectRequest(name = "proj", path = "/path")
    val request2 = RegisterProjectRequest(name = "proj", path = "/path")
    val request3 = RegisterProjectRequest(name = "other", path = "/path")

    assertEquals(request1, request2)
    assertNotEquals(request1, request3)
  }
}
