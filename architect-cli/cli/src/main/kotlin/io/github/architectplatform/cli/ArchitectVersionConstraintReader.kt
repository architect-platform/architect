package io.github.architectplatform.cli

import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.core.project.app.ConfigLoader
import io.github.architectplatform.core.project.infra.YamlConfigParser

object ArchitectVersionConstraintReader {
  fun read(projectPath: String, profile: String?): String? {
    val loader = ConfigLoader(YamlConfigParser())
    val config = loader.loadWithRaw(projectPath, profile)?.config ?: return null
    return config.getKey<String>("architect.version")?.trim()?.ifBlank { null }
  }
}
