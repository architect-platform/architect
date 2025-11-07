package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Version information response from the engine.
 */
@Serdeable
data class VersionInfo(
  val version: String,
  val component: String = "engine"
)
