package io.github.architectplatform.core.plugin.domain.events

import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.PluginEvent
import io.github.architectplatform.core.domain.events.PluginEventType
import io.github.architectplatform.core.domain.events.PluginId

object PluginEvents {

  data class PluginEventDTO(
      override val pluginEventType: PluginEventType,
      override val pluginId: PluginId,
  ) : PluginEvent

  fun pluginDownloadCompleted(
      pluginId: PluginId,
  ): ArchitectEvent<PluginEventDTO> =
      ArchitectEventDTO(
          "plugin.download.completed",
          PluginEventDTO(
              pluginEventType = PluginEventType.DOWNLOAD_COMPLETED,
              pluginId = pluginId,
          ))

  fun pluginDownloadSkipped(
      pluginId: PluginId,
  ): ArchitectEvent<PluginEventDTO> =
      ArchitectEventDTO(
          "plugin.download.skipped",
          PluginEventDTO(
              pluginEventType = PluginEventType.DOWNLOAD_SKIPPED,
              pluginId = pluginId,
          ),
      )

  fun pluginDownloadStarted(
      pluginId: PluginId,
  ): ArchitectEvent<PluginEventDTO> =
      ArchitectEventDTO(
          "plugin.download.started",
          PluginEventDTO(
              pluginEventType = PluginEventType.DOWNLOAD_STARTED,
              pluginId = pluginId,
          ),
      )

  fun pluginDownloadFailed(
      pluginId: PluginId,
  ): ArchitectEvent<PluginEventDTO> =
      ArchitectEventDTO(
          "plugin.download.failed",
          PluginEventDTO(
              pluginEventType = PluginEventType.DOWNLOAD_FAILED,
              pluginId = pluginId,
          ))

  fun pluginLoaded(
      pluginId: PluginId,
  ): ArchitectEvent<PluginEventDTO> =
      ArchitectEventDTO(
          "plugin.loaded",
          PluginEventDTO(
              pluginEventType = PluginEventType.LOADED,
              pluginId = pluginId,
          ))
}
