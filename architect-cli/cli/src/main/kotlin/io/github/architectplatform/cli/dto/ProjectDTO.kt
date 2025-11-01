package io.github.architectplatform.cli.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ProjectDTO(
    val name: String,
    val path: String,
    val context: ProjectContextDTO,
) {
  @Serdeable
  data class ProjectContextDTO(
      val dir: String,
      val config: Map<String, Any>,
  )
}
