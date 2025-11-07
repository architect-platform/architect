package io.github.architectplatform.engine.core.version.interfaces

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

/**
 * API controller for version information.
 */
@Controller("/api/version")
class VersionApiController {

  companion object {
    private const val ENGINE_VERSION = "1.6.1"
  }

  data class VersionInfo(
    val version: String,
    val component: String = "engine"
  )

  /**
   * Returns the current engine version.
   */
  @Get
  fun getVersion(): VersionInfo {
    return VersionInfo(version = ENGINE_VERSION)
  }
}
