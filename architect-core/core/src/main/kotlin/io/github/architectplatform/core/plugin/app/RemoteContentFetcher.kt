package io.github.architectplatform.core.plugin.app

/**
 * Host-provided network adapter used by core to fetch remote text/binary content.
 *
 * Core defines the contract but does not implement protocol-specific transport.
 */
interface RemoteContentFetcher {
  fun fetchText(url: String, headers: Map<String, String> = emptyMap()): String

  fun fetchBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray
}
