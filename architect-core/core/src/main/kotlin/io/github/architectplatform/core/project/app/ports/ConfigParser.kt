package io.github.architectplatform.core.project.app.ports

import io.github.architectplatform.api.core.project.Config

interface ConfigParser {
  fun parse(context: String): Config
}
