package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class TaskDTO(
    val id: String,
) {
  override fun toString(): String {
    return id
  }
}
