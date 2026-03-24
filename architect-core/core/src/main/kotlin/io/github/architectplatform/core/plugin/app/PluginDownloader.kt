package io.github.architectplatform.core.plugin.app

import java.io.File

interface PluginDownloader {
  fun download(url: String): File
}
