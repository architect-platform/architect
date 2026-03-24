package io.github.architectplatform.core.plugin.app

import com.fasterxml.jackson.annotation.JsonProperty

data class PluginConfig(
    var name: String,
    val version: String = "latest",
    val assetType: String = "jar",
    val asset: String = "$name.$assetType",
    val type: String = "github",
    val path: String = ".",
    val owner: String = "architect-platform",
    val repo: String = "$owner/$name",
    val pattern: String = "$name-",
    val registry: String? = null,
    val url: String? = null,
    val sha256: String? = null,
    @JsonProperty("verify-signature")
    val verifySignature: Boolean = false,
    @JsonProperty("trusted-keys")
    val trustedKeys: List<String> = emptyList(),
    val command: String? = null,
    @JsonProperty("package")
    val packageName: String? = null,
)
